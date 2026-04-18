package com.sshtools.synergy.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileAdapter;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.server.InMemoryPasswordAuthenticator;
import com.sshtools.server.SshServer;

/**
 * Integration tests that verify the SFTP subsystem makes the correct
 * server-side filesystem calls in response to SFTP protocol operations.
 *
 * <p>A {@link RecordingFileFactory} wraps a real {@link com.sshtools.common.files.direct.NioFileFactory
 * NioFileFactory} and records the names of the {@link AbstractFile} methods
 * that are invoked.  Each test exercises one SFTP operation and then asserts
 * that the expected filesystem method was recorded.
 */
@DisplayName("SFTP – server-side filesystem call recording")
class SftpMockFsIT {

    // -----------------------------------------------------------------------
    // Recording infrastructure
    // -----------------------------------------------------------------------

    /**
     * Shared queue that accumulates the name of every recorded
     * {@link AbstractFile} method call across all file instances.
     */
    private static final Queue<String> CALLS = new ConcurrentLinkedQueue<>();

    /**
     * An {@link AbstractFile} decorator that records key method invocations
     * before delegating to the wrapped delegate file.
     */
    static final class RecordingFile extends AbstractFileAdapter {

        private final AbstractFileFactory<?> factory;

        RecordingFile(AbstractFile delegate, AbstractFileFactory<?> factory) {
            super(delegate);
            this.factory = factory;
        }

        @Override
        public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
            return factory;
        }

        // -- recorded methods ------------------------------------------------

        @Override
        public OutputStream getOutputStream() throws IOException, PermissionDeniedException {
            CALLS.add("getOutputStream");
            return file.getOutputStream();
        }

        @Override
        public OutputStream getOutputStream(boolean append)
                throws IOException, PermissionDeniedException {
            CALLS.add("getOutputStream");
            return file.getOutputStream(append);
        }

        @Override
        public InputStream getInputStream() throws IOException, PermissionDeniedException {
            CALLS.add("getInputStream");
            return file.getInputStream();
        }

        @Override
        public boolean createFolder() throws IOException, PermissionDeniedException {
            CALLS.add("createFolder");
            return file.createFolder();
        }

        @Override
        public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
            CALLS.add("getChildren");
            return file.getChildren();
        }

        @Override
        public boolean delete(boolean recursive) throws IOException, PermissionDeniedException {
            CALLS.add("delete");
            return file.delete(recursive);
        }

        @Override
        public SftpFileAttributes getAttributes()
                throws IOException, PermissionDeniedException {
            CALLS.add("getAttributes");
            return file.getAttributes();
        }

        @Override
        public void setAttributes(SftpFileAttributes attrs) throws IOException {
            CALLS.add("setAttributes");
            file.setAttributes(attrs);
        }

        @Override
        public AbstractFileRandomAccess openFile(boolean writeAccess)
                throws IOException, PermissionDeniedException {
            CALLS.add("openFile");
            return file.openFile(writeAccess);
        }

        @Override
        public AbstractFile resolveFile(String child)
                throws IOException, PermissionDeniedException {
            return new RecordingFile(file.resolveFile(child), factory);
        }

        @Override
        public AbstractFile getParentFile()
                throws IOException, PermissionDeniedException {
            return new RecordingFile(file.getParentFile(), factory);
        }

        @Override
        public void moveTo(AbstractFile target)
                throws IOException, PermissionDeniedException {
            CALLS.add("moveTo");
            // Unwrap the target so the delegate can handle it.
            if (target instanceof RecordingFile) {
                file.moveTo(((RecordingFile) target).file);
            } else {
                file.moveTo(target);
            }
        }
    }

    /**
     * An {@link AbstractFileFactory} that wraps a real NioFileFactory and
     * returns {@link RecordingFile} instances for every lookup.
     */
    static final class RecordingFileFactory implements AbstractFileFactory<RecordingFile> {

        private final AbstractFileFactory<? extends AbstractFile> delegate;

        RecordingFileFactory(AbstractFileFactory<? extends AbstractFile> delegate) {
            this.delegate = delegate;
        }

        @Override
        public RecordingFile getFile(String path)
                throws PermissionDeniedException, IOException {
            return new RecordingFile(delegate.getFile(path), this);
        }
    }

    // -----------------------------------------------------------------------
    // Server setup / teardown
    // -----------------------------------------------------------------------

    private static SshServer server;
    private static int serverPort;
    private static SshKeyPair clientKeyPair;
    private static Path tempRoot;

    static final String TEST_USER     = "mockuser";
    static final String TEST_PASSWORD = "M0ck!test";

    @BeforeAll
    static void startServer() throws Exception {
        tempRoot      = Files.createTempDirectory("sftp-mock-fs-");
        clientKeyPair = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);
        SshKeyPair hostKey = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);

        AbstractFileFactory<? extends AbstractFile> nioFactory =
            NioFileFactoryBuilder.create()
                .withHome(tempRoot.toFile())
                .withoutSandbox()
                .build();
        RecordingFileFactory recording = new RecordingFileFactory(nioFactory);

        server = new SshServer(0);
        server.addHostKey(hostKey);
        server.addAuthenticator(
            new InMemoryPasswordAuthenticator()
                .addUser(TEST_USER, TEST_PASSWORD.toCharArray()));
        server.setFileFactory(con -> recording);
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

    @BeforeEach
    void clearCalls() {
        CALLS.clear();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private SshClient connect() throws IOException, SshException {
        return SshClientBuilder.create()
            .withHostname("127.0.0.1")
            .withPort(serverPort)
            .withUsername(TEST_USER)
            .withPassword(TEST_PASSWORD)
            .onConfigure(ctx -> ctx.setHostKeyVerification((h, pk) -> true))
            .build();
    }

    private SftpClient openSftp(SshClient ssh)
            throws SshException, PermissionDeniedException, IOException {
        SftpClient sftp = SftpClientBuilder.create().withClient(ssh).build();
        try {
            sftp.cd("");   // initialise CWD to the server's default home directory
        } catch (SftpStatusException e) {
            throw new IOException("Cannot initialise SFTP home directory", e);
        }
        return sftp;
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Uploading a file via put() triggers openFile on the server filesystem")
    void upload_triggersOpenFile(@TempDir Path localDir) throws Exception {
        Path src = localDir.resolve("mock-upload.txt");
        Files.write(src, "mock upload content".getBytes(StandardCharsets.UTF_8));

        try (SshClient ssh = connect();
             SftpClient sftp = openSftp(ssh)) {
            sftp.put(src.toString(), "mock-upload.txt");
        }

        assertTrue(CALLS.contains("openFile"),
                "uploading a file must trigger openFile on the server-side filesystem");
    }

    @Test
    @DisplayName("Downloading a file via get() triggers openFile on the server filesystem")
    void download_triggersOpenFile(@TempDir Path localDir) throws Exception {
        // Seed a file directly on the server side so we can download it.
        Files.write(tempRoot.resolve("to-download.txt"),
                "content to download".getBytes(StandardCharsets.UTF_8));

        Path dest = localDir.resolve("downloaded.txt");

        try (SshClient ssh = connect();
             SftpClient sftp = openSftp(ssh)) {
            sftp.get("to-download.txt", dest.toString());
        }

        assertTrue(CALLS.contains("openFile"),
                "downloading a file must trigger openFile on the server-side filesystem");
    }

    @Test
    @DisplayName("Creating a directory via mkdir() triggers createFolder on the server filesystem")
    void mkdir_triggersCreateFolder() throws Exception {
        String dirName = "mock-dir-" + System.nanoTime();

        try (SshClient ssh = connect();
             SftpClient sftp = openSftp(ssh)) {
            sftp.mkdir(dirName);
        }

        assertTrue(CALLS.contains("createFolder"),
                "mkdir() must trigger createFolder on the server-side filesystem");
    }

    @Test
    @DisplayName("Listing a directory via ls() triggers getChildren on the server filesystem")
    void ls_triggersGetChildren() throws Exception {
        try (SshClient ssh = connect();
             SftpClient sftp = openSftp(ssh)) {
            SftpFile[] files = sftp.ls(".");
            assertNotNull(files, "ls() must return a non-null listing");
        }

        assertTrue(CALLS.contains("getChildren"),
                "ls() must trigger getChildren on the server-side filesystem");
    }

    @Test
    @DisplayName("Deleting a file via rm() triggers delete on the server filesystem")
    void rm_triggersDelete(@TempDir Path localDir) throws Exception {
        Path src = localDir.resolve("to-delete.txt");
        Files.write(src, "delete me".getBytes(StandardCharsets.UTF_8));

        try (SshClient ssh = connect();
             SftpClient sftp = openSftp(ssh)) {
            sftp.put(src.toString(), "to-delete.txt");
            CALLS.clear();           // only care about the delete, not the upload
            sftp.rm("to-delete.txt");
        }

        assertTrue(CALLS.contains("delete"),
                "rm() must trigger delete on the server-side filesystem");
    }

    @Test
    @DisplayName("stat() triggers getAttributes on the server filesystem")
    void stat_triggersGetAttributes(@TempDir Path localDir) throws Exception {
        Path src = localDir.resolve("stat-target.txt");
        Files.write(src, "stat me".getBytes(StandardCharsets.UTF_8));

        try (SshClient ssh = connect();
             SftpClient sftp = openSftp(ssh)) {
            sftp.put(src.toString(), "stat-target.txt");
            CALLS.clear();           // only care about the stat, not the upload
            assertNotNull(sftp.stat("stat-target.txt"), "stat() must return non-null attributes");
        }

        assertTrue(CALLS.contains("getAttributes"),
                "stat() must trigger getAttributes on the server-side filesystem");
    }

    @Test
    @DisplayName("Rename via rename() triggers moveTo on the server filesystem")
    void rename_triggersMoveTo(@TempDir Path localDir) throws Exception {
        Path src = localDir.resolve("rename-src.txt");
        Files.write(src, "rename me".getBytes(StandardCharsets.UTF_8));

        try (SshClient ssh = connect();
             SftpClient sftp = openSftp(ssh)) {
            sftp.put(src.toString(), "rename-src.txt");
            CALLS.clear();           // only care about the rename, not the upload
            sftp.rename("rename-src.txt", "rename-dst.txt");
        }

        assertTrue(CALLS.contains("moveTo"),
                "rename() must trigger moveTo on the server-side filesystem");
    }

    @Test
    @DisplayName("Upload + download round-trip through recording factory preserves content")
    void uploadDownload_contentPreserved(@TempDir Path localDir) throws Exception {
        byte[] content = "Hello from mock FS recording!".getBytes(StandardCharsets.UTF_8);
        Path src  = localDir.resolve("round-trip.txt");
        Path dest = localDir.resolve("round-trip-down.txt");
        Files.write(src, content);

        try (SshClient ssh = connect();
             SftpClient sftp = openSftp(ssh)) {
            sftp.put(src.toString(), "round-trip.txt");
            sftp.get("round-trip.txt", dest.toString());
        }

        assertArrayEquals(content, Files.readAllBytes(dest),
                "content must be identical after upload+download through recording factory");
    }
}
