package com.sshtools.synergy.tests;

/*-
 * #%L
 * Integration Tests
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

/**
 * Integration tests covering SFTP subsystem operations: file upload/download,
 * directory listing, directory creation, rename, and deletion.
 */
@DisplayName("SFTP operations")
class SftpIT extends AbstractSshIntegrationTest {

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    private SftpClient openSftp(SshClient ssh)
            throws SshException, PermissionDeniedException, IOException {
        SftpClient sftp = SftpClientBuilder.create().withClient(ssh).build();
        try {
            sftp.cd("");   // initialise CWD to the server's default home directory
        } catch (com.sshtools.common.sftp.SftpStatusException e) {
            throw new IOException("Cannot initialise SFTP home directory", e);
        }
        return sftp;
    }

    // ------------------------------------------------------------------ //
    //  Upload / download                                                  //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("File uploaded with put() can be retrieved intact with get()")
    void uploadAndDownload(@TempDir Path localDir) throws Exception {
        byte[] content = "Hello, Maverick SFTP!".getBytes(StandardCharsets.UTF_8);
        Path localUpload = localDir.resolve("upload.txt");
        Files.write(localUpload, content);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            sftp.put(localUpload.toString(), "upload.txt");

            Path localDownload = localDir.resolve("download.txt");
            sftp.get("upload.txt", localDownload.toString());

            assertArrayEquals(content, Files.readAllBytes(localDownload),
                    "downloaded content should match uploaded content");
        }
    }

    @Test
    @DisplayName("Large file (1 MB) round-trips without data corruption")
    void largeFileRoundTrip(@TempDir Path localDir) throws Exception {
        byte[] content = new byte[1024 * 1024];
        // Deterministic pseudo-random fill so assertion is meaningful
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i & 0xFF);
        }
        Path localUpload = localDir.resolve("large.bin");
        Files.write(localUpload, content);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            sftp.put(localUpload.toString(), "large.bin");

            Path localDownload = localDir.resolve("large-download.bin");
            sftp.get("large.bin", localDownload.toString());

            assertArrayEquals(content, Files.readAllBytes(localDownload),
                    "large file content should survive round-trip");
        }
    }

    // ------------------------------------------------------------------ //
    //  Directory listing                                                  //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("ls() on root returns non-null, non-empty array after uploading a file")
    void lsAfterUpload(@TempDir Path localDir) throws Exception {
        byte[] content = "ls test".getBytes(StandardCharsets.UTF_8);
        Path localFile = localDir.resolve("listed.txt");
        Files.write(localFile, content);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            sftp.put(localFile.toString(), "listed.txt");

            SftpFile[] entries = sftp.ls();
            assertNotNull(entries);
            boolean found = Arrays.stream(entries)
                    .map(SftpFile::getFilename)
                    .collect(Collectors.toSet())
                    .contains("listed.txt");
            assertTrue(found, "ls() should include the uploaded file");
        }
    }

    // ------------------------------------------------------------------ //
    //  Directory creation                                                 //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("mkdir() creates a directory that appears in ls()")
    void mkdirCreatesDirectory() throws Exception {
        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            String dirName = "testdir-" + System.nanoTime();
            sftp.mkdir(dirName);

            SftpFile[] entries = sftp.ls();
            assertNotNull(entries);
            boolean found = Arrays.stream(entries)
                    .map(SftpFile::getFilename)
                    .collect(Collectors.toSet())
                    .contains(dirName);
            assertTrue(found, "mkdir'd directory should appear in ls()");
        }
    }

    // ------------------------------------------------------------------ //
    //  Rename                                                             //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("rename() moves a file to its new name in ls()")
    void renameFile(@TempDir Path localDir) throws Exception {
        byte[] content = "rename me".getBytes(StandardCharsets.UTF_8);
        Path localFile = localDir.resolve("before.txt");
        Files.write(localFile, content);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            sftp.put(localFile.toString(), "before.txt");
            sftp.rename("before.txt", "after.txt");

            SftpFile[] entries = sftp.ls();
            assertNotNull(entries);

            java.util.Set<String> names = Arrays.stream(entries)
                    .map(SftpFile::getFilename)
                    .collect(Collectors.toSet());

            assertTrue(names.contains("after.txt"), "renamed file should appear under new name");
        }
    }

    // ------------------------------------------------------------------ //
    //  Deletion                                                           //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("rm() removes a file so it no longer appears in ls()")
    void rmDeletesFile(@TempDir Path localDir) throws Exception {
        byte[] content = "delete me".getBytes(StandardCharsets.UTF_8);
        Path localFile = localDir.resolve("todelete.txt");
        Files.write(localFile, content);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            sftp.put(localFile.toString(), "todelete.txt");

            // Verify it exists before deletion
            SftpFile[] before = sftp.ls();
            boolean existsBefore = Arrays.stream(before)
                    .map(SftpFile::getFilename)
                    .collect(Collectors.toSet())
                    .contains("todelete.txt");
            assertTrue(existsBefore, "file should exist before deletion");

            sftp.rm("todelete.txt");

            SftpFile[] after = sftp.ls();
            boolean existsAfter = Arrays.stream(after)
                    .map(SftpFile::getFilename)
                    .collect(Collectors.toSet())
                    .contains("todelete.txt");
            assertTrue(!existsAfter, "file should not exist after rm()");
        }
    }

    // ------------------------------------------------------------------ //
    //  Public-key auth works with SFTP                                   //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("SFTP session via public-key auth functions correctly")
    void sftpWithPublicKeyAuth(@TempDir Path localDir) throws Exception {
        byte[] content = "pubkey sftp test".getBytes(StandardCharsets.UTF_8);
        Path localFile = localDir.resolve("pubkey.txt");
        Files.write(localFile, content);

        try (SshClient ssh = connectWithPublicKey();
             SftpClient sftp = openSftp(ssh)) {

            sftp.put(localFile.toString(), "pubkey.txt");

            Path localDownload = localDir.resolve("pubkey-dl.txt");
            sftp.get("pubkey.txt", localDownload.toString());

            assertArrayEquals(content, Files.readAllBytes(localDownload));
        }
    }

    // ------------------------------------------------------------------ //
    //  Error cases                                                        //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("get() of a non-existent remote file throws SftpStatusException")
    void getOfMissingFileThrows(@TempDir Path localDir) throws Exception {
        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            org.junit.jupiter.api.Assertions.assertThrows(SftpStatusException.class, () ->
                sftp.get("no-such-file-" + System.nanoTime() + ".txt",
                         localDir.resolve("out.txt").toString())
            );
        }
    }

    // ------------------------------------------------------------------ //
    //  Edge-case error handling                                           //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("rm() of a non-existent file throws SftpStatusException")
    void rmOfNonExistentFileThrows() throws Exception {
        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            assertThrows(SftpStatusException.class,
                    () -> sftp.rm("no-such-file-" + System.nanoTime() + ".txt"));
        }
    }

    @Test
    @DisplayName("rmdir() of a non-existent directory throws SftpStatusException")
    void rmdirOfNonExistentThrows() throws Exception {
        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            assertThrows(SftpStatusException.class,
                    () -> sftp.rmdir("no-such-dir-" + System.nanoTime()));
        }
    }

    // ------------------------------------------------------------------ //
    //  File attributes                                                    //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("stat() returns non-null attributes with correct size for an uploaded file")
    void statReturnsAttributesForUploadedFile(@TempDir Path localDir) throws Exception {
        byte[] content = "stat test content".getBytes(StandardCharsets.UTF_8);
        Path localFile = localDir.resolve("stat-test.txt");
        Files.write(localFile, content);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            sftp.put(localFile.toString(), "stat-test.txt");
            SftpFileAttributes attrs = sftp.stat("stat-test.txt");

            assertNotNull(attrs, "stat() must return non-null attributes");
            assertEquals(content.length, attrs.size().longValue(),
                    "stat() size must match uploaded file size");
        }
    }

    // ------------------------------------------------------------------ //
    //  Partial download                                                   //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("get() with a position offset downloads only bytes from that offset onward")
    void partialDownloadViaOutputStreamPosition(@TempDir Path localDir) throws Exception {
        byte[] fullContent = "ABCDE12345FGHIJ".getBytes(StandardCharsets.UTF_8);
        long offset = 5;  // skip the first 5 bytes ("ABCDE")
        Path localFile = localDir.resolve("partial-src.txt");
        Files.write(localFile, fullContent);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            sftp.put(localFile.toString(), "partial.txt");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            sftp.get("partial.txt", out, null, offset);

            byte[] expected = Arrays.copyOfRange(fullContent, (int) offset, fullContent.length);
            assertArrayEquals(expected, out.toByteArray(),
                    "download from offset must return only bytes from that position onward");
        }
    }
}
