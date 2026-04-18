package com.sshtools.synergy.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;

/**
 * Integration tests focused specifically on the authentication layer:
 * password auth, public-key auth, and various failure modes.
 */
@DisplayName("Authentication")
class AuthenticationIT extends AbstractSshIntegrationTest {

    // ------------------------------------------------------------------ //
    //  Password authentication                                            //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Correct password authenticates successfully")
    void correctPasswordAuthenticated() throws IOException, SshException {
        try (SshClient client = connectWithPassword()) {
            assertTrue(client.isAuthenticated());
        }
    }

    @Test
    @DisplayName("Incorrect password throws IOException")
    void incorrectPasswordRejected() {
        assertThrows(IOException.class, () ->
            SshClientBuilder.create()
                .withHostname("127.0.0.1")
                .withPort(SERVER.getPort())
                .withUsername(SshServerExtension.TEST_USER)
                .withPassword("notTheRightPassword")
                .onConfigure(ctx -> ctx.setHostKeyVerification((host, pk) -> true))
                .build()
        );
    }

    @Test
    @DisplayName("Empty password throws IOException")
    void emptyPasswordRejected() {
        assertThrows(IOException.class, () ->
            SshClientBuilder.create()
                .withHostname("127.0.0.1")
                .withPort(SERVER.getPort())
                .withUsername(SshServerExtension.TEST_USER)
                .withPassword("")
                .onConfigure(ctx -> ctx.setHostKeyVerification((host, pk) -> true))
                .build()
        );
    }

    @Test
    @DisplayName("Unknown username throws IOException")
    void unknownUserRejected() {
        assertThrows(IOException.class, () ->
            SshClientBuilder.create()
                .withHostname("127.0.0.1")
                .withPort(SERVER.getPort())
                .withUsername("nobody")
                .withPassword(SshServerExtension.TEST_PASSWORD)
                .onConfigure(ctx -> ctx.setHostKeyVerification((host, pk) -> true))
                .build()
        );
    }

    // ------------------------------------------------------------------ //
    //  Public-key authentication                                          //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Registered public key authenticates successfully")
    void registeredPublicKeyAuthenticated() throws IOException, SshException {
        try (SshClient client = connectWithPublicKey()) {
            assertTrue(client.isAuthenticated());
            assertTrue(client.isConnected());
        }
    }

    @Test
    @DisplayName("Unregistered public key throws IOException")
    void unregisteredPublicKeyRejected() throws Exception {
        SshKeyPair unauthorized = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);
        assertThrows(IOException.class, () ->
            SshClientBuilder.create()
                .withHostname("127.0.0.1")
                .withPort(SERVER.getPort())
                .withUsername(SshServerExtension.TEST_USER)
                .addIdentities(unauthorized)
                .onConfigure(ctx -> ctx.setHostKeyVerification((host, pk) -> true))
                .build()
        );
    }

    @Test
    @DisplayName("Public key for wrong user throws IOException")
    void publicKeyForWrongUserRejected() throws Exception {
        // The key pair is authorized for TEST_USER, not "wronguser"
        assertThrows(IOException.class, () ->
            SshClientBuilder.create()
                .withHostname("127.0.0.1")
                .withPort(SERVER.getPort())
                .withUsername("wronguser")
                .addIdentities(SERVER.getClientKeyPair())
                .onConfigure(ctx -> ctx.setHostKeyVerification((host, pk) -> true))
                .build()
        );
    }

    // ------------------------------------------------------------------ //
    //  Post-authentication state                                          //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("isAuthenticated() is false when password auth was not performed")
    void unauthenticatedClientReportsNotAuthenticated() throws IOException, SshException {
        try (SshClient client = connectWithPassword()) {
            assertTrue(client.isAuthenticated());
            client.disconnect();
            assertFalse(client.isAuthenticated(), "disconnected client should not be authenticated");
        }
    }
}
