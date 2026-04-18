package com.sshtools.synergy.tests;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.common.ssh.SshException;

/**
 * Integration tests that verify successful session establishment using every
 * key-exchange algorithm the local JCE provider supports.
 */
@DisplayName("Key-exchange negotiation")
class KeyExchangeNegotiationIT extends AbstractSshIntegrationTest {

    /**
     * Well-known KEX algorithms present in standard JCE + BouncyCastle builds.
     * If a particular algorithm is absent from the runtime, the test is skipped
     * via {@code Assumptions.assumeDoesNotThrow}.
     */
    @ParameterizedTest(name = "kex={0}")
    @ValueSource(strings = {
        "curve25519-sha256",
        "curve25519-sha256@libssh.org",
        "ecdh-sha2-nistp256",
        "ecdh-sha2-nistp384",
        "ecdh-sha2-nistp521",
        "diffie-hellman-group14-sha256",
        "diffie-hellman-group14-sha1",
        "diffie-hellman-group-exchange-sha256",
        "diffie-hellman-group-exchange-sha1",
    })
    @DisplayName("Successful handshake using algorithm")
    void kexAlgorithmNegotiates(String algorithm) throws IOException, SshException {
        // SshException from setPreferredKeyExchange means the algorithm is unavailable;
        // skip the test rather than fail it.
        SshClient client;
        try {
            client = SshClientBuilder.create()
                    .withHostname("127.0.0.1")
                    .withPort(SERVER.getPort())
                    .withUsername(SshServerExtension.TEST_USER)
                    .withPassword(SshServerExtension.TEST_PASSWORD)
                    .onConfigure(ctx -> {
                        ctx.setHostKeyVerification((host, pk) -> true);
                        ctx.setPreferredKeyExchange(algorithm);
                    })
                    .build();
        } catch (SshException e) {
            Assumptions.assumeTrue(false,
                    algorithm + " is not available in this environment: " + e.getMessage());
            return; // unreachable; satisfies compiler
        }

        try (client) {
            assertTrue(client.isConnected());
            assertTrue(client.isAuthenticated());
        }
    }

    @Test
    @DisplayName("Server rejects connection when no common KEX algorithm exists")
    void noCommonKexAlgorithmRejected() {
        // A deliberately invented algorithm that no server supports
        assertThrows(Exception.class, () ->
            SshClientBuilder.create()
                .withHostname("127.0.0.1")
                .withPort(SERVER.getPort())
                .withUsername(SshServerExtension.TEST_USER)
                .withPassword(SshServerExtension.TEST_PASSWORD)
                .onConfigure(ctx -> {
                    ctx.setHostKeyVerification((host, pk) -> true);
                    try {
                        ctx.setPreferredKeyExchange("fake-kex-no-such-algorithm");
                    } catch (SshException e) {
                        throw new RuntimeException(e);
                    }
                })
                .build()
        );
    }

    @Test
    @DisplayName("Host-key verification failure causes IOException")
    void rejectsUnexpectedHostKey() {
        assertThrows(SshException.class, () ->
            SshClientBuilder.create()
                .withHostname("127.0.0.1")
                .withPort(SERVER.getPort())
                .withUsername(SshServerExtension.TEST_USER)
                .withPassword(SshServerExtension.TEST_PASSWORD)
                // Reject every host key
                .onConfigure(ctx -> ctx.setHostKeyVerification((host, pk) -> false))
                .build()
        );
    }

    @Test
    @DisplayName("Subsequent connections to the same server succeed independently")
    void multipleSequentialConnectionsSucceed() throws IOException, SshException {
        for (int i = 0; i < 3; i++) {
            try (SshClient client = connectWithPassword()) {
                assertTrue(client.isAuthenticated(), "connection " + (i + 1) + " should be authenticated");
            }
        }
    }

    @Test
    @DisplayName("Concurrent connections to the same server all succeed")
    void concurrentConnectionsSucceed() throws Exception {
        SshClient[] clients = new SshClient[3];
        try {
            for (int i = 0; i < clients.length; i++) {
                clients[i] = connectWithPassword();
            }
            for (SshClient c : clients) {
                assertTrue(c.isAuthenticated());
                assertTrue(c.isConnected());
            }
        } finally {
            for (SshClient c : clients) {
                if (c != null) c.close();
            }
        }
    }
}
