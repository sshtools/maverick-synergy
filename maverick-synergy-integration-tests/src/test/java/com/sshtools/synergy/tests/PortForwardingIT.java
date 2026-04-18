package com.sshtools.synergy.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.common.forwarding.ForwardingPolicy;
import com.sshtools.common.ssh.SshException;

/**
 * Integration tests for local TCP port-forwarding via SSH.
 *
 * <p>Each test starts an in-process echo server, creates an SSH local-forward
 * tunnel to that echo server, and then verifies that data sent through the
 * tunnel is correctly echoed back.
 */
@DisplayName("Port forwarding")
class PortForwardingIT extends AbstractSshIntegrationTest {

    /**
     * Opens a password-authenticated connection with the client-side
     * {@link ForwardingPolicy} configured to allow local forwarding.
     * Without this, {@link SshClient#startLocalForwarding} throws
     * {@code UnauthorizedException} before even contacting the server.
     */
    private SshClient connectWithForwarding() throws IOException, SshException {
        return SshClientBuilder.create()
            .withHostname("127.0.0.1")
            .withPort(SERVER.getPort())
            .withUsername(SshServerExtension.TEST_USER)
            .withPassword(SshServerExtension.TEST_PASSWORD)
            .onConfigure(ctx -> {
                ctx.setHostKeyVerification((host, pk) -> true);
                ctx.getForwardingPolicy().allowForwarding();
            })
            .build();
    }

    private ServerSocket echoServerSocket;
    private ExecutorService echoExecutor;
    private AtomicBoolean echoRunning;

    // -----------------------------------------------------------------------
    // Echo server lifecycle
    // -----------------------------------------------------------------------

    /**
     * Starts a minimal TCP echo server bound to a random OS-assigned port on
     * loopback.  The server reads up to 1 KB from each accepted connection and
     * writes the same bytes back before closing.
     */
    @BeforeEach
    void startEchoServer() throws IOException {
        echoRunning = new AtomicBoolean(true);
        echoServerSocket = new ServerSocket(0);   // OS picks port
        echoExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "echo-server");
            t.setDaemon(true);
            return t;
        });
        echoExecutor.submit(() -> {
            while (echoRunning.get()) {
                try {
                    echoServerSocket.setSoTimeout(200);
                    Socket client;
                    try {
                        client = echoServerSocket.accept();
                    } catch (java.net.SocketTimeoutException e) {
                        continue;   // check echoRunning flag again
                    }
                    try (Socket c = client) {
                        InputStream in  = c.getInputStream();
                        OutputStream out = c.getOutputStream();
                        byte[] buf = new byte[1024];
                        int n = in.read(buf);
                        if (n > 0) {
                            out.write(buf, 0, n);
                            out.flush();
                        }
                    }
                } catch (IOException e) {
                    if (echoRunning.get()) {
                        // Unexpected error — log but continue
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @AfterEach
    void stopEchoServer() throws IOException {
        echoRunning.set(false);
        echoExecutor.shutdownNow();
        if (echoServerSocket != null && !echoServerSocket.isClosed()) {
            echoServerSocket.close();
        }
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("startLocalForwarding() returns a positive port number")
    void startLocalForwarding_returnsPositivePort() throws Exception {
        int echoPort = echoServerSocket.getLocalPort();

        try (SshClient ssh = connectWithForwarding()) {
            int localPort = ssh.startLocalForwarding("127.0.0.1", 0, "127.0.0.1", echoPort);
            assertTrue(localPort > 0,
                    "startLocalForwarding() must return a positive port; got " + localPort);
            ssh.stopLocalForwarding("127.0.0.1", localPort);
        }
    }

    @Test
    @DisplayName("Data sent through local forward tunnel is echoed back intact")
    void localForward_dataRoundTrips() throws Exception {
        int echoPort = echoServerSocket.getLocalPort();
        byte[] payload = "tunnel-test-payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        CountDownLatch receivedLatch = new CountDownLatch(1);
        byte[] received = new byte[payload.length];

        try (SshClient ssh = connectWithForwarding()) {
            int localPort = ssh.startLocalForwarding("127.0.0.1", 0, "127.0.0.1", echoPort);
            assertTrue(localPort > 0, "local forwarding port must be positive");

            // Connect through the SSH tunnel to the echo server
            try (Socket tunnel = new Socket("127.0.0.1", localPort)) {
                tunnel.setSoTimeout(5000);
                OutputStream out = tunnel.getOutputStream();
                InputStream  in  = tunnel.getInputStream();

                out.write(payload);
                out.flush();

                int offset = 0;
                while (offset < received.length) {
                    int n = in.read(received, offset, received.length - offset);
                    if (n < 0) break;
                    offset += n;
                }
                receivedLatch.countDown();
            }

            ssh.stopLocalForwarding("127.0.0.1", localPort);
        }

        assertTrue(receivedLatch.await(0, TimeUnit.SECONDS),
                "should have received echo before closing tunnel socket");
        assertArrayEquals(payload, received,
                "data received through tunnel must match sent payload");
    }

    @Test
    @DisplayName("Forwarding can be established and torn down multiple times")
    void forwardingMultipleTimes() throws Exception {
        int echoPort = echoServerSocket.getLocalPort();

        try (SshClient ssh = connectWithForwarding()) {
            for (int i = 0; i < 3; i++) {
                int localPort = ssh.startLocalForwarding("127.0.0.1", 0, "127.0.0.1", echoPort);
                assertTrue(localPort > 0, "iteration " + i + ": port must be positive");
                ssh.stopLocalForwarding("127.0.0.1", localPort);
            }
        }
    }

    @Test
    @DisplayName("Large payload (16 KB) through tunnel is echoed back without corruption")
    void largePayload_roundTrips() throws Exception {
        // Use a dedicated one-shot frame-based echo server to avoid needing
        // shutdownOutput(), which does not propagate cleanly through SSH tunnels.
        byte[] payload = new byte[16 * 1024];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }

        try (ServerSocket frameEchoServer = new ServerSocket(0)) {
            int echoPort = frameEchoServer.getLocalPort();

            // One-shot frame echo: reads a 4-byte length, then that many bytes, writes them back.
            Thread echoThread = new Thread(() -> {
                try (Socket client = frameEchoServer.accept()) {
                    DataInputStream  din  = new DataInputStream(client.getInputStream());
                    DataOutputStream dout = new DataOutputStream(client.getOutputStream());
                    int len = din.readInt();
                    byte[] buf = new byte[len];
                    din.readFully(buf);
                    dout.writeInt(len);
                    dout.write(buf);
                    dout.flush();
                } catch (IOException e) { /* server side closed */ }
            }, "frame-echo");
            echoThread.setDaemon(true);
            echoThread.start();

            try (SshClient ssh = connectWithForwarding()) {
                int localPort = ssh.startLocalForwarding("127.0.0.1", 0, "127.0.0.1", echoPort);
                assertTrue(localPort > 0, "local forwarding port must be positive");

                byte[] received;
                try (Socket tunnel = new Socket("127.0.0.1", localPort)) {
                    tunnel.setSoTimeout(10_000);
                    DataOutputStream out = new DataOutputStream(tunnel.getOutputStream());
                    DataInputStream  in  = new DataInputStream(tunnel.getInputStream());

                    out.writeInt(payload.length);
                    out.write(payload);
                    out.flush();

                    int returnedLen = in.readInt();
                    received = new byte[returnedLen];
                    in.readFully(received);
                }

                ssh.stopLocalForwarding("127.0.0.1", localPort);
                assertArrayEquals(payload, received, "16 KB payload must round-trip without corruption");
            }
        }
    }

    @Test
    @DisplayName("Two independent SSH connections can forward simultaneously to the same target")
    void twoConnections_forwardSimultaneously() throws Exception {
        int echoPort = echoServerSocket.getLocalPort();
        byte[] p1 = "connection-one".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] p2 = "connection-two".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        try (SshClient ssh1 = connectWithForwarding();
             SshClient ssh2 = connectWithForwarding()) {

            int port1 = ssh1.startLocalForwarding("127.0.0.1", 0, "127.0.0.1", echoPort);
            int port2 = ssh2.startLocalForwarding("127.0.0.1", 0, "127.0.0.1", echoPort);
            assertTrue(port1 > 0, "ssh1 local port must be positive");
            assertTrue(port2 > 0, "ssh2 local port must be positive");

            byte[] r1 = sendAndReceive("127.0.0.1", port1, p1);
            byte[] r2 = sendAndReceive("127.0.0.1", port2, p2);

            assertArrayEquals(p1, r1, "payload via connection-1 must echo correctly");
            assertArrayEquals(p2, r2, "payload via connection-2 must echo correctly");

            ssh1.stopLocalForwarding("127.0.0.1", port1);
            ssh2.stopLocalForwarding("127.0.0.1", port2);
        }
    }

    // -----------------------------------------------------------------------
    // Private utilities
    // -----------------------------------------------------------------------

    /** Sends {@code payload} to {@code host:port} and returns the echoed bytes. */
    private static byte[] sendAndReceive(String host, int port, byte[] payload) throws IOException {
        byte[] received = new byte[payload.length];
        try (Socket s = new Socket(host, port)) {
            s.setSoTimeout(5000);
            s.getOutputStream().write(payload);
            s.getOutputStream().flush();
            int offset = 0;
            while (offset < received.length) {
                int n = s.getInputStream().read(received, offset, received.length - offset);
                if (n < 0) break;
                offset += n;
            }
        }
        return received;
    }
}
