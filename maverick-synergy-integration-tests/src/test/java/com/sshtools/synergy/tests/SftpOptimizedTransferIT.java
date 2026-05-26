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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

/**
 * Integration coverage for optimized SFTP transfer behavior.
 */
@DisplayName("SFTP optimized transfers")
class SftpOptimizedTransferIT extends AbstractSshIntegrationTest {

    private static final int OPT_BLOCK_SIZE = 4096;

    @AfterEach
    void clearOptimizationTelemetry() {
        System.clearProperty("maverick.read.optimizedBlock");
        System.clearProperty("maverick.read.asyncRequests");
        System.clearProperty("maverick.write.optimizedBlock");
        System.clearProperty("maverick.write.asyncRequestsMax");
    }

    @Test
    @DisplayName("Optimized put uses configured block size and async request count")
    void optimizedPutHonoursConfiguredRequestWindow(@TempDir Path localDir) throws Exception {
        byte[] content = createContent(256 * 1024);
        Path localUpload = localDir.resolve("optimized-put.bin");
        Path localDownload = localDir.resolve("optimized-put-download.bin");
        Files.write(localUpload, content);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh, 3)) {

            sftp.put(localUpload.toString(), "optimized-put.bin");

            assertEquals(String.valueOf(OPT_BLOCK_SIZE), System.getProperty("maverick.write.optimizedBlock"));
            assertEquals("3", System.getProperty("maverick.write.asyncRequestsMax"));

            sftp.get("optimized-put.bin", localDownload.toString());
        }

        assertArrayEquals(content, Files.readAllBytes(localDownload));
    }

    @Test
    @DisplayName("Optimized get uses configured block size and async request count")
    void optimizedGetHonoursConfiguredRequestWindow(@TempDir Path localDir) throws Exception {
        byte[] content = createContent(256 * 1024);
        Path localUpload = localDir.resolve("optimized-get-source.bin");
        Path localDownload = localDir.resolve("optimized-get-download.bin");
        Files.write(localUpload, content);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh, 1)) {

            sftp.put(localUpload.toString(), "optimized-get.bin");
            System.clearProperty("maverick.read.optimizedBlock");
            System.clearProperty("maverick.read.asyncRequests");

            sftp.get("optimized-get.bin", localDownload.toString());

            assertEquals(String.valueOf(OPT_BLOCK_SIZE), System.getProperty("maverick.read.optimizedBlock"));
            assertEquals("1", System.getProperty("maverick.read.asyncRequests"));
        }

        assertArrayEquals(content, Files.readAllBytes(localDownload));
    }

    @Test
    @DisplayName("Optimized put supports cancellation via progress callback")
    void optimizedPutCanBeCancelled(@TempDir Path localDir) throws Exception {
        byte[] content = createContent(2 * 1024 * 1024);
        Path localUpload = localDir.resolve("cancel-put.bin");
        Files.write(localUpload, content);

        FileTransferProgress cancellingProgress = cancelAfter(64 * 1024);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh, 4)) {

            assertThrows(TransferCancelledException.class,
                    () -> sftp.put(localUpload.toString(), "cancel-put.bin", cancellingProgress));
        }
    }

    @Test
    @DisplayName("Optimized get supports cancellation via progress callback")
    void optimizedGetCanBeCancelled(@TempDir Path localDir) throws Exception {
        byte[] content = createContent(2 * 1024 * 1024);
        Path localUpload = localDir.resolve("cancel-get-source.bin");
        Files.write(localUpload, content);

        FileTransferProgress cancellingProgress = cancelAfter(64 * 1024);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh, 4)) {

            sftp.put(localUpload.toString(), "cancel-get.bin");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertThrows(TransferCancelledException.class,
                    () -> sftp.get("cancel-get.bin", out, cancellingProgress, 0));
        }
    }

    @Test
    @DisplayName("Configured optimized transfer client remains valid for directory operations")
    void configuredTransferClientStillSupportsOtherOperations(@TempDir Path localDir) throws Exception {
        byte[] content = createContent(64 * 1024);
        Path localUpload = localDir.resolve("ops-file.bin");
        Files.write(localUpload, content);

        String dirName = "optimized-ops-" + System.nanoTime();
        String fileName = dirName + "/ops-file.bin";

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh, 2)) {

            sftp.mkdir(dirName);
            sftp.put(localUpload.toString(), fileName);

            SftpFile[] files = sftp.ls(dirName);
            Set<String> names = Arrays.stream(files)
                    .map(SftpFile::getFilename)
                    .collect(Collectors.toSet());
            assertTrue(names.contains("ops-file.bin"));

            sftp.rm(fileName);
            sftp.rmdir(dirName);
        }
    }

    // ------------------------------------------------------------------ //
    //  Edge-case coverage                                                 //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Zero-byte file round-trips without error using optimized client")
    void zeroByteFileRoundTrip(@TempDir Path localDir) throws Exception {
        Path localUpload = localDir.resolve("empty.bin");
        Files.createFile(localUpload);   // 0-byte file
        Path localDownload = localDir.resolve("empty-dl.bin");

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh, 2)) {

            sftp.put(localUpload.toString(), "empty.bin");
            sftp.get("empty.bin", localDownload.toString());
        }

        assertArrayEquals(new byte[0], Files.readAllBytes(localDownload));
    }

    @Test
    @DisplayName("File exactly one block in size round-trips correctly via synchronous-only write path")
    void exactBlockSizeFileRoundTrip(@TempDir Path localDir) throws Exception {
        byte[] content = createContent(OPT_BLOCK_SIZE);
        Path localUpload = localDir.resolve("exact-block.bin");
        Files.write(localUpload, content);
        Path localDownload = localDir.resolve("exact-block-dl.bin");

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh, 2)) {

            sftp.put(localUpload.toString(), "exact-block.bin");
            sftp.get("exact-block.bin", localDownload.toString());
        }

        assertArrayEquals(content, Files.readAllBytes(localDownload));
    }

    @Test
    @DisplayName("Uploading to an existing remote path overwrites content correctly")
    void overwriteExistingRemoteFile(@TempDir Path localDir) throws Exception {
        byte[] original    = createContent(64 * 1024);
        byte[] replacement = createContent(128 * 1024);
        Path v1 = localDir.resolve("overwrite-v1.bin");
        Path v2 = localDir.resolve("overwrite-v2.bin");
        Path downloaded = localDir.resolve("overwrite-dl.bin");
        Files.write(v1, original);
        Files.write(v2, replacement);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh, 2)) {

            sftp.put(v1.toString(), "overwrite.bin");
            sftp.put(v2.toString(), "overwrite.bin");
            sftp.get("overwrite.bin", downloaded.toString());
        }

        assertArrayEquals(replacement, Files.readAllBytes(downloaded));
    }

    @Test
    @DisplayName("Progress callback receives started, progressed, and completed events on put")
    void progressCallbackLifecycleOnPut(@TempDir Path localDir) throws Exception {
        byte[] content = createContent(128 * 1024);
        Path localUpload = localDir.resolve("lifecycle.bin");
        Files.write(localUpload, content);

        List<String> events = new ArrayList<>();
        AtomicLong lastProgress = new AtomicLong();

        FileTransferProgress trackingProgress = new FileTransferProgress() {
            @Override
            public void started(long bytesTotal, String file) {
                events.add("started");
            }
            @Override
            public void progressed(long bytesSoFar) {
                lastProgress.set(bytesSoFar);
                events.add("progressed");
            }
            @Override
            public void completed() {
                events.add("completed");
            }
        };

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh, 2)) {

            sftp.put(localUpload.toString(), "lifecycle.bin", trackingProgress);
        }

        assertEquals("started", events.get(0),  "first event must be started");
        assertEquals("completed", events.get(events.size() - 1), "last event must be completed");
        assertTrue(events.contains("progressed"), "at least one progressed event must fire");
        assertTrue(lastProgress.get() > 0, "progressed bytes must be positive");
    }

    // ------------------------------------------------------------------ //
    //  Resume / auto-size coverage                                        //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Put with non-zero position appends to existing remote content (resume-upload path)")
    void resumePutFromNonZeroPositionAppendsContent(@TempDir Path localDir) throws Exception {
        byte[] part1 = createContent(64 * 1024);
        byte[] part2 = createContent(64 * 1024);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh, 2)) {

            // Upload part1 as the initial file content
            sftp.put(new ByteArrayInputStream(part1), "resume.bin", 0);

            // Append part2 starting at the end of part1 (simulating a resume)
            sftp.put(new ByteArrayInputStream(part2), "resume.bin", (long) part1.length);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            sftp.get("resume.bin", out);

            byte[] expected = new byte[part1.length + part2.length];
            System.arraycopy(part1, 0, expected, 0, part1.length);
            System.arraycopy(part2, 0, expected, part1.length, part2.length);

            assertArrayEquals(expected, out.toByteArray());
        }
    }

    @Test
    @DisplayName("asyncRequests=0 triggers automatic window-based sizing of the async request count")
    void autoSizedAsyncRequestsWhenZeroConfigured(@TempDir Path localDir) throws Exception {
        System.clearProperty("maverick.write.asyncRequestsMax");

        byte[] content = createContent(128 * 1024);
        Path localUpload = localDir.resolve("auto-size.bin");
        Files.write(localUpload, content);

        try (SshClient ssh = connectWithPassword();
             SftpClient sftp = openSftp(ssh, 0)) {   // 0 → auto-size

            sftp.put(localUpload.toString(), "auto-size.bin");
        }

        String autoMax = System.getProperty("maverick.write.asyncRequestsMax");
        assertNotNull(autoMax, "asyncRequestsMax telemetry must be set even when 0 is configured");
        assertTrue(Integer.parseInt(autoMax) > 0, "auto-sized asyncRequestsMax must be a positive value");
    }

    private SftpClient openSftp(SshClient ssh, int asyncRequests)
            throws SshException, PermissionDeniedException, IOException {
        SftpClient sftp = SftpClientBuilder.create()
                .withClient(ssh)
                .withBlockSize(OPT_BLOCK_SIZE)
                .withAsyncRequests(asyncRequests)
                .build();
        try {
            sftp.cd("");
        } catch (SftpStatusException e) {
            throw new IOException("Cannot initialise SFTP home directory", e);
        }
        return sftp;
    }

    private static byte[] createContent(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i * 31);
        }
        return data;
    }

    private static FileTransferProgress cancelAfter(long thresholdBytes) {
        AtomicLong progressed = new AtomicLong();
        return new FileTransferProgress() {
            @Override
            public void progressed(long bytesSoFar) {
                progressed.set(bytesSoFar);
            }

            @Override
            public boolean isCancelled() {
                return progressed.get() >= thresholdBytes;
            }
        };
    }
}
