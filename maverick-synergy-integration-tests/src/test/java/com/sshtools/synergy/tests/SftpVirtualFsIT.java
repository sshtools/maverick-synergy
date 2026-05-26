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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMountTemplate;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.server.InMemoryPasswordAuthenticator;
import com.sshtools.server.InMemoryPublicKeyAuthenticator;
import com.sshtools.server.SshServer;

/**
 * Integration tests for SFTP operations using a {@link VirtualFileFactory}
 * as the server-side filesystem.  The virtual FS mounts a temporary directory
 * through a {@link NioFileFactoryBuilder NioFileFactory} wrapped by the
 * VirtualFileFactory, isolating the SFTP layer from any direct NIO path
 * handling on the server side.
 *
 * <p>This test class starts its own dedicated in-process server so it can
 * configure the {@code VirtualFileFactory} independently of the shared
 * server used by other IT classes.
 */
@DisplayName("SFTP – VirtualFileFactory backend")
class SftpVirtualFsIT {

    private static SshServer server;
    private static SshKeyPair clientKeyPair;
    private static int serverPort;
    private static Path tempRoot;

    static final String TEST_USER     = "vfsuser";
    static final String TEST_PASSWORD = "vfsP@ssword1";

    @BeforeAll
    static void startServer() throws Exception {
        tempRoot    = Files.createTempDirectory("sftp-vfs-it-");
        clientKeyPair = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);
        SshKeyPair hostKey = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);

        server = new SshServer(0);
        server.addHostKey(hostKey);
        server.addAuthenticator(
            new InMemoryPasswordAuthenticator().addUser(TEST_USER, TEST_PASSWORD.toCharArray()));
        server.addAuthenticator(
            new InMemoryPublicKeyAuthenticator().addAuthorizedKey(TEST_USER, clientKeyPair.getPublicKey()));

        final Path root = tempRoot;
        server.setFileFactory(con -> {
            try {
                return new VirtualFileFactory(
                    new VirtualMountTemplate("/", root.toAbsolutePath().toString(),
                        NioFileFactoryBuilder.create()
                            .withHome(root.toFile())
                            .withoutSandbox()
                            .build(),
                        false));
            } catch (PermissionDeniedException e) {
                throw new IOException("Cannot create VirtualFileFactory", e);
            }
        });

        server.start();
        serverPort = server.getPort();
    }

    @AfterAll
    static void stopServer() throws IOException {
        if (server != null && server.isRunning()) {
            server.stop();
        }
        if (tempRoot != null) {
            Files.walk(tempRoot)
                 .sorted(java.util.Comparator.reverseOrder())
                 .forEach(p -> p.toFile().delete());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private SshClient connectWithPassword() throws IOException, SshException {
        return SshClientBuilder.create()
            .withHostname("127.0.0.1")
            .withPort(serverPort)
            .withUsername(TEST_USER)
            .withPassword(TEST_PASSWORD)
            .onConfigure(ctx -> ctx.setHostKeyVerification((h, pk) -> true))
            .build();
    }

    private SshClient connectWithPublicKey() throws IOException, SshException {
        return SshClientBuilder.create()
            .withHostname("127.0.0.1")
            .withPort(serverPort)
            .withUsername(TEST_USER)
            .addIdentities(clientKeyPair)
            .onConfigure(ctx -> ctx.setHostKeyVerification((h, pk) -> true))
            .build();
    }

    private SftpClient openSftp(SshClient ssh) throws SshException, PermissionDeniedException, IOException {
        SftpClient sftp = SftpClientBuilder.create().withClient(ssh).build();
        try {
            sftp.cd("");   // initialise CWD to the server's default home directory
        } catch (com.sshtools.common.sftp.SftpStatusException e) {
            throw new IOException("Cannot initialise SFTP home directory", e);
        }
        return sftp;
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("File uploaded with put() is retrieved intact via get() (VFS backend)")
    void virtualFs_uploadAndDownload(@TempDir Path localDir) throws Exception {
        byte[] content = "VirtualFileFactory SFTP test".getBytes(StandardCharsets.UTF_8);
        Path upload   = localDir.resolve("vfs-upload.txt");
        Files.write(upload, content);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            sftp.put(upload.toString(), "vfs-upload.txt");

            Path download = localDir.resolve("vfs-download.txt");
            sftp.get("vfs-upload.txt", download.toString());

            assertArrayEquals(content, Files.readAllBytes(download),
                    "downloaded content must match uploaded content via VirtualFileFactory");
        }
    }

    @Test
    @DisplayName("mkdir() creates a directory visible in the subsequent ls() listing")
    void virtualFs_mkdir_listable(@TempDir Path localDir) throws Exception {
        String dirName = "vfs-newdir";

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            sftp.mkdir(dirName);

            SftpFile[] files = sftp.ls(".");
            assertNotNull(files, "ls() must return non-null");
            assertTrue(files.length > 0, "ls() must return at least one entry after mkdir");

            boolean found = Arrays.stream(files)
                                  .anyMatch(f -> f.getFilename().equals(dirName));
            assertTrue(found, "newly created directory '" + dirName + "' must appear in ls()");
        }
    }

    @Test
    @DisplayName("rename() renames a file and the new name is visible in ls()")
    void virtualFs_rename(@TempDir Path localDir) throws Exception {
        Path src = localDir.resolve("before.txt");
        Files.write(src, "rename-test".getBytes(StandardCharsets.UTF_8));

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            sftp.put(src.toString(), "before.txt");
            sftp.rename("before.txt", "after.txt");

            SftpFile[] files = sftp.ls(".");
            String names = Arrays.stream(files)
                                 .map(SftpFile::getFilename)
                                 .collect(Collectors.joining(", "));

            assertFalse(Arrays.stream(files).anyMatch(f -> f.getFilename().equals("before.txt")),
                    "original name must not appear in ls() after rename; found: " + names);
            assertTrue(Arrays.stream(files).anyMatch(f -> f.getFilename().equals("after.txt")),
                    "new name must appear in ls() after rename; found: " + names);
        }
    }

    @Test
    @DisplayName("rm() removes a file and it is no longer visible in ls()")
    void virtualFs_delete(@TempDir Path localDir) throws Exception {
        Path src = localDir.resolve("todelete.txt");
        Files.write(src, "delete-test".getBytes(StandardCharsets.UTF_8));

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            sftp.put(src.toString(), "todelete.txt");
            sftp.rm("todelete.txt");

            SftpFile[] files = sftp.ls(".");
            boolean stillPresent = files != null &&
                Arrays.stream(files).anyMatch(f -> f.getFilename().equals("todelete.txt"));
            assertFalse(stillPresent, "deleted file must not appear in ls()");
        }
    }

    @Test
    @DisplayName("Public-key auth opens a SFTP session against VirtualFileFactory backend")
    void virtualFs_pubkeyAuth_sftp(@TempDir Path localDir) throws Exception {
        byte[] content = "pubkey-vfs".getBytes(StandardCharsets.UTF_8);
        Path src = localDir.resolve("pubkey-vfs.txt");
        Files.write(src, content);

        try (SshClient ssh = connectWithPublicKey();
             SftpClient sftp = openSftp(ssh)) {

            assertTrue(ssh.isAuthenticated(), "must be authenticated via public key");
            sftp.put(src.toString(), "pubkey-vfs.txt");

            Path download = localDir.resolve("pubkey-vfs-down.txt");
            sftp.get("pubkey-vfs.txt", download.toString());
            assertArrayEquals(content, Files.readAllBytes(download));
        }
    }

    @Test
    @DisplayName("Stat on non-existent file throws SftpStatusException")
    void virtualFs_statNonExistent() throws Exception {
        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh)) {

            org.junit.jupiter.api.Assertions.assertThrows(
                SftpStatusException.class,
                () -> sftp.stat("__no_such_file__"),
                "stat on a non-existent file must throw SftpStatusException"
            );
        }
    }
}
