package com.sshtools.synergy.tests;

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
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMountTemplate;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.server.InMemoryPasswordAuthenticator;
import com.sshtools.server.SshServer;

/**
 * Parameterized integration tests for SFTP operations verifying that all core
 * filesystem operations (upload, download, list, mkdir, rename, delete) work
 * identically across different server-side {@link AbstractFileFactory}
 * implementations.
 *
 * <p>Each {@link ParameterizedTest} case receives a named {@link FsFixture}
 * that bundles a freshly-configured in-process {@link SshServer} with the
 * specific filesystem backend under test.  The server is started before the
 * test body runs and stopped in {@link FsFixture#close()}, so it lives only
 * for the duration of a single parameterized iteration.
 */
@DisplayName("SFTP – filesystem implementation matrix")
class SftpFilesystemIT {

    // -----------------------------------------------------------------------
    // Fixture
    // -----------------------------------------------------------------------

    /**
     * AutoCloseable test fixture that owns one SshServer instance configured
     * with a specific filesystem backend.
     */
    static final class FsFixture implements AutoCloseable {

        final String name;
        final SshServer server;
        final SshKeyPair clientKeyPair;

        static final String USER     = "fstest";
        static final String PASSWORD = "Fs!test99";

        FsFixture(String name, Path tempDir, AbstractFileFactory<?> factory) throws Exception {
            this.name = name;
            this.clientKeyPair = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);
            SshKeyPair hostKey = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);

            server = new SshServer(0);
            server.addHostKey(hostKey);
            server.addAuthenticator(
                new InMemoryPasswordAuthenticator().addUser(USER, PASSWORD.toCharArray()));
            // FileFactory: return the provided AbstractFileFactory for any connection
            server.setFileFactory(con -> factory);
            server.start();
        }

        SshClient connect() throws IOException, SshException {
            return SshClientBuilder.create()
                .withHostname("127.0.0.1")
                .withPort(server.getPort())
                .withUsername(USER)
                .withPassword(PASSWORD)
                .onConfigure(ctx -> ctx.setHostKeyVerification((h, pk) -> true))
                .build();
        }

        @Override
        public void close() {
            if (server.isRunning()) {
                server.stop();
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // -----------------------------------------------------------------------
    // Factory method for parameterized cases
    // -----------------------------------------------------------------------

    /**
     * Returns one {@link FsFixture} per filesystem implementation to test.
     * JUnit 5's {@code @MethodSource} streams these as test arguments.
     */
    static Stream<FsFixture> fileSystemFixtures() throws Exception {
        // ── NioFileFactory ──────────────────────────────────────────────
        Path nioTemp = Files.createTempDirectory("sftp-nio-");
        var nioFactory = NioFileFactoryBuilder.create()
            .withHome(nioTemp.toFile())
            .withoutSandbox()
            .build();
        FsFixture nioFixture = new FsFixture("NioFileFactory", nioTemp, nioFactory);

        // ── VirtualFileFactory (backed by NioFileFactory) ───────────────
        Path vfsTemp = Files.createTempDirectory("sftp-vfs-");
        AbstractFileFactory<?> vfsInner = NioFileFactoryBuilder.create()
            .withHome(vfsTemp.toFile())
            .withoutSandbox()
            .build();
        AbstractFileFactory<?> vfsFactory;
        try {
            vfsFactory = new VirtualFileFactory(
                new VirtualMountTemplate("/", vfsTemp.toAbsolutePath().toString(),
                    vfsInner, false));
        } catch (PermissionDeniedException e) {
            throw new IOException("Cannot create VirtualFileFactory for test parameter", e);
        }
        FsFixture vfsFixture = new FsFixture("VirtualFileFactory", vfsTemp, vfsFactory);

        return Stream.of(nioFixture, vfsFixture);
    }

    // -----------------------------------------------------------------------
    // Tests (one fixture per filesystem)
    // -----------------------------------------------------------------------

    /** Builds an {@link SftpClient} and initialises its CWD to the server's home directory. */
    private static SftpClient buildSftp(SshClient ssh)
            throws SshException, PermissionDeniedException, IOException {
        SftpClient sftp = SftpClientBuilder.create().withClient(ssh).build();
        try {
            sftp.cd("");   // resolve server default directory so relative paths work
        } catch (com.sshtools.common.sftp.SftpStatusException e) {
            throw new IOException("Cannot initialise SFTP home directory", e);
        }
        return sftp;
    }

    @ParameterizedTest(name = "{0}: upload+download round-trip")
    @MethodSource("fileSystemFixtures")
    @DisplayName("File uploaded with put() is retrieved intact via get()")
    void uploadAndDownload(FsFixture fs) throws Exception {
        byte[] content = "round-trip test".getBytes(StandardCharsets.UTF_8);
        Path local = Files.createTempFile("up-", ".txt");
        Path down  = Files.createTempFile("down-", ".txt");
        Files.write(local, content);
        try (fs; SshClient ssh = fs.connect();
             SftpClient sftp = buildSftp(ssh)) {

            sftp.put(local.toString(), "rt.txt");
            sftp.get("rt.txt", down.toString());
            assertArrayEquals(content, Files.readAllBytes(down),
                    fs + ": downloaded bytes must match uploaded bytes");
        } finally {
            Files.deleteIfExists(local);
            Files.deleteIfExists(down);
        }
    }

    @ParameterizedTest(name = "{0}: mkdir creates a listable directory")
    @MethodSource("fileSystemFixtures")
    @DisplayName("mkdir() creates a directory that appears in ls()")
    void mkdirAppearsInLs(FsFixture fs) throws Exception {
        try (fs; SshClient ssh = fs.connect();
             SftpClient sftp = buildSftp(ssh)) {

            sftp.mkdir("newdir");
            SftpFile[] files = sftp.ls(".");
            assertNotNull(files, fs + ": ls() must return non-null");
            boolean found = Arrays.stream(files).anyMatch(f -> f.getFilename().equals("newdir"));
            assertTrue(found, fs + ": 'newdir' must appear in ls() after mkdir");
        }
    }

    @ParameterizedTest(name = "{0}: rename changes the filename")
    @MethodSource("fileSystemFixtures")
    @DisplayName("rename() renames a file and the old name disappears from ls()")
    void renameChangesFilename(FsFixture fs) throws Exception {
        Path src = Files.createTempFile("rename-src-", ".txt");
        Files.write(src, "rename-payload".getBytes(StandardCharsets.UTF_8));
        try (fs; SshClient ssh = fs.connect();
             SftpClient sftp = buildSftp(ssh)) {

            sftp.put(src.toString(), "old-name.txt");
            sftp.rename("old-name.txt", "new-name.txt");

            SftpFile[] files = sftp.ls(".");
            String names = Arrays.stream(files)
                                 .map(SftpFile::getFilename)
                                 .collect(Collectors.joining(", "));

            assertFalse(Arrays.stream(files).anyMatch(f -> f.getFilename().equals("old-name.txt")),
                    fs + ": old name must be absent after rename; found: " + names);
            assertTrue(Arrays.stream(files).anyMatch(f -> f.getFilename().equals("new-name.txt")),
                    fs + ": new name must appear after rename; found: " + names);
        } finally {
            Files.deleteIfExists(src);
        }
    }

    @ParameterizedTest(name = "{0}: delete removes file from ls()")
    @MethodSource("fileSystemFixtures")
    @DisplayName("rm() removes a file and it no longer appears in ls()")
    void deleteRemovesFile(FsFixture fs) throws Exception {
        Path src = Files.createTempFile("del-", ".txt");
        Files.write(src, "will-be-deleted".getBytes(StandardCharsets.UTF_8));
        try (fs; SshClient ssh = fs.connect();
             SftpClient sftp = buildSftp(ssh)) {

            sftp.put(src.toString(), "todelete.txt");
            sftp.rm("todelete.txt");

            SftpFile[] files = sftp.ls(".");
            boolean present = files != null &&
                Arrays.stream(files).anyMatch(f -> f.getFilename().equals("todelete.txt"));
            assertFalse(present, fs + ": deleted file must not appear in ls()");
        } finally {
            Files.deleteIfExists(src);
        }
    }

    @ParameterizedTest(name = "{0}: stat on non-existent file throws SftpStatusException")
    @MethodSource("fileSystemFixtures")
    @DisplayName("stat() on a non-existent path throws SftpStatusException")
    void statNonExistentThrows(FsFixture fs) throws Exception {
        try (fs; SshClient ssh = fs.connect();
             SftpClient sftp = buildSftp(ssh)) {

            org.junit.jupiter.api.Assertions.assertThrows(
                SftpStatusException.class,
                () -> sftp.stat("__nonexistent_" + System.nanoTime()),
                fs + ": stat on nonexistent file must throw SftpStatusException"
            );
        }
    }

    @ParameterizedTest(name = "{0}: large file (512 KB) round-trips without corruption")
    @MethodSource("fileSystemFixtures")
    @DisplayName("Large-file (512 KB) upload+download round-trip is lossless")
    void largeFileRoundTrip(FsFixture fs) throws Exception {
        byte[] content = new byte[512 * 1024];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i & 0xFF);
        }
        Path src  = Files.createTempFile("large-src-", ".bin");
        Path dest = Files.createTempFile("large-dst-", ".bin");
        Files.write(src, content);
        try (fs; SshClient ssh = fs.connect();
             SftpClient sftp = buildSftp(ssh)) {

            sftp.put(src.toString(), "large.bin");
            sftp.get("large.bin", dest.toString());
            assertArrayEquals(content, Files.readAllBytes(dest),
                    fs + ": 512 KB file must survive round-trip");
        } finally {
            Files.deleteIfExists(src);
            Files.deleteIfExists(dest);
        }
    }
}
