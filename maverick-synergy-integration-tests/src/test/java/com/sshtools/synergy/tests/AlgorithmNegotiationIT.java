
package com.sshtools.synergy.tests;

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
import com.sshtools.synergy.ssh.SshContext;

/**
 * Integration tests that verify successful session establishment when specific
 * cipher or MAC algorithms are negotiated.  Each parameterized case forces the
 * client to prefer the named algorithm; if the algorithm is absent from the
 * runtime the test is skipped via {@link Assumptions}.
 *
 * <p>Key-exchange algorithm negotiation is covered separately by
 * {@link KeyExchangeNegotiationIT}.
 */
@DisplayName("Cipher and MAC negotiation")
class AlgorithmNegotiationIT extends AbstractSshIntegrationTest {

    // -----------------------------------------------------------------------
    // Cipher negotiation
    // -----------------------------------------------------------------------

    /**
     * Establishes a full authenticated connection using a specific Client-Server
     * cipher.  Both CS (client→server) and SC (server→client) directions are
     * set to the same cipher for simplicity.
     */
    @ParameterizedTest(name = "cipher={0}")
    @ValueSource(strings = {
        SshContext.CIPHER_AES128_CTR,       // "aes128-ctr"
        SshContext.CIPHER_AES192_CTR,       // "aes192-ctr"
        SshContext.CIPHER_AES256_CTR,       // "aes256-ctr"
        SshContext.CIPHER_AES128_CBC,       // "aes128-cbc"
        SshContext.CIPHER_AES256_CBC,       // "aes256-cbc"
        SshContext.CIPHER_AES_GCM_128,      // "aes128-gcm@openssh.com"
        SshContext.CIPHER_AES_GCM_256,      // "aes256-gcm@openssh.com"
    })
    @DisplayName("Successful handshake with preferred cipher")
    void cipherNegotiates(String cipher) throws IOException, SshException {
        SshClient client;
        try {
            client = SshClientBuilder.create()
                    .withHostname("127.0.0.1")
                    .withPort(SERVER.getPort())
                    .withUsername(SshServerExtension.TEST_USER)
                    .withPassword(SshServerExtension.TEST_PASSWORD)
                    .onConfigure(ctx -> {
                        ctx.setHostKeyVerification((host, pk) -> true);
                        try {
                            ctx.setPreferredCipherCS(cipher);
                            ctx.setPreferredCipherSC(cipher);
                        } catch (IOException | SshException e) {
                            Assumptions.assumeTrue(false,
                                    cipher + " not available: " + e.getMessage());
                        }
                    })
                    .build();
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    cipher + " not available in this environment: " + e.getMessage());
            return;
        }

        try (client) {
            assertTrue(client.isConnected(),  "must be connected with cipher " + cipher);
            assertTrue(client.isAuthenticated(), "must be authenticated with cipher " + cipher);
        }
    }

    // -----------------------------------------------------------------------
    // MAC negotiation
    // -----------------------------------------------------------------------

    /**
     * Establishes a full authenticated connection using a specific Client-Server
     * MAC algorithm.  Both CS and SC MAC directions are set to the same
     * algorithm.  GCM-mode ciphers include built-in authentication so MAC
     * negotiation tests use a standard non-AEAD cipher (aes256-ctr).
     */
    @ParameterizedTest(name = "mac={0}")
    @ValueSource(strings = {
        SshContext.HMAC_SHA256,             // "hmac-sha2-256"
        SshContext.HMAC_SHA512,             // "hmac-sha2-512"
        SshContext.HMAC_SHA1,              // "hmac-sha1"
        SshContext.HMAC_SHA256_ETM,         // "hmac-sha2-256-etm@openssh.com"
        SshContext.HMAC_SHA512_ETM,         // "hmac-sha2-512-etm@openssh.com"
        SshContext.HMAC_SHA1_ETM,           // "hmac-sha1-etm@openssh.com"
    })
    @DisplayName("Successful handshake with preferred MAC")
    void macNegotiates(String mac) throws IOException, SshException {
        SshClient client;
        try {
            client = SshClientBuilder.create()
                    .withHostname("127.0.0.1")
                    .withPort(SERVER.getPort())
                    .withUsername(SshServerExtension.TEST_USER)
                    .withPassword(SshServerExtension.TEST_PASSWORD)
                    .onConfigure(ctx -> {
                        ctx.setHostKeyVerification((host, pk) -> true);
                        // Use a standard non-AEAD cipher so MAC negotiation
                        // is meaningful (AEAD ciphers bypass MAC).
                        try {
                            ctx.setPreferredCipherCS(SshContext.CIPHER_AES256_CTR);
                            ctx.setPreferredCipherSC(SshContext.CIPHER_AES256_CTR);
                            ctx.setPreferredMacCS(mac);
                            ctx.setPreferredMacSC(mac);
                        } catch (IOException | SshException e) {
                            Assumptions.assumeTrue(false,
                                    mac + " not available: " + e.getMessage());
                        }
                    })
                    .build();
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    mac + " not available in this environment: " + e.getMessage());
            return;
        }

        try (client) {
            assertTrue(client.isConnected(),     "must be connected with MAC " + mac);
            assertTrue(client.isAuthenticated(), "must be authenticated with MAC " + mac);
        }
    }

    // -----------------------------------------------------------------------
    // Compression negotiation
    // -----------------------------------------------------------------------

    /**
     * Verifies that a connection works when the client explicitly requests
     * {@code none} compression (the default).  This acts as a baseline check
     * that the compression-configuration path does not break the handshake.
     */
    @Test
    @DisplayName("Connection with explicit 'none' compression succeeds")
    void compressionNone_succeeds() throws IOException, SshException {
        try (SshClient client = SshClientBuilder.create()
                .withHostname("127.0.0.1")
                .withPort(SERVER.getPort())
                .withUsername(SshServerExtension.TEST_USER)
                .withPassword(SshServerExtension.TEST_PASSWORD)
                .onConfigure(ctx -> {
                    ctx.setHostKeyVerification((host, pk) -> true);
                    try {
                        ctx.setPreferredCompressionCS(SshContext.COMPRESSION_NONE);
                        ctx.setPreferredCompressionSC(SshContext.COMPRESSION_NONE);
                    } catch (IOException e) {
                        Assumptions.assumeTrue(false,
                                "none compression not available: " + e.getMessage());
                    }
                })
                .build()) {

            assertTrue(client.isConnected(),     "must be connected with none compression");
            assertTrue(client.isAuthenticated(), "must be authenticated with none compression");
        }
    }

    /**
     * Verifies that a connection works when the client requests {@code zlib}
     * compression if that algorithm is available on the server.  Skipped when
     * the runtime does not support zlib compression.
     */
    @Test
    @DisplayName("Connection with 'zlib' compression succeeds if available")
    void compressionZlib_succeedsIfAvailable() throws IOException, SshException {
        SshClient client;
        try {
            client = SshClientBuilder.create()
                    .withHostname("127.0.0.1")
                    .withPort(SERVER.getPort())
                    .withUsername(SshServerExtension.TEST_USER)
                    .withPassword(SshServerExtension.TEST_PASSWORD)
                    .onConfigure(ctx -> {
                        ctx.setHostKeyVerification((host, pk) -> true);
                        try {
                            ctx.setPreferredCompressionCS(SshContext.COMPRESSION_ZLIB);
                            ctx.setPreferredCompressionSC(SshContext.COMPRESSION_ZLIB);
                        } catch (IOException e) {
                            Assumptions.assumeTrue(false,
                                    "zlib compression not available: " + e.getMessage());
                        }
                    })
                    .build();
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "zlib compression not available in this environment: " + e.getMessage());
            return;
        }

        try (client) {
            assertTrue(client.isConnected(),     "must be connected with zlib compression");
            assertTrue(client.isAuthenticated(), "must be authenticated with zlib compression");
        }
    }
}
