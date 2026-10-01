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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sshtools.client.AuthenticationMessage;
import com.sshtools.client.ClientAuthenticator;
import com.sshtools.client.SimpleClientAuthenticator;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.TransportProtocolClient;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * Integration tests focused specifically on the authentication layer:
 * password auth, public-key auth, and various failure modes.
 */
@DisplayName("Authentication")
class AuthenticationIT extends AbstractSshIntegrationTest {

    private static final int SSH_MSG_USERAUTH_REQUEST = 50;
    private static final int SSH_MSG_USERAUTH_PK_OK = 60;

    /**
     * Intentionally crafted authenticator that probes with one username and signs with another.
     * This test currently fails because the server incorrectly accepts the second username.
     */
    private static final class UsernameSwitchPublicKeyAuthenticator extends SimpleClientAuthenticator implements ClientAuthenticator {

        private final SshKeyPair keyPair;
        private final String firstUsername;
        private final String secondUsername;
        private TransportProtocolClient transport;

        private UsernameSwitchPublicKeyAuthenticator(SshKeyPair keyPair, String firstUsername, String secondUsername) {
            this.keyPair = keyPair;
            this.firstUsername = firstUsername;
            this.secondUsername = secondUsername;
        }

        @Override
        public String getName() {
            return "publickey";
        }

        @Override
        public void authenticate(TransportProtocolClient transport, String username) throws IOException, SshException {
            this.transport = transport;
            sendRequest(firstUsername, false);
        }

        @Override
        public boolean processMessage(ByteArrayReader msg) throws IOException, SshException {
            int type = msg.read() & 0xFF;
            if (type == SSH_MSG_USERAUTH_PK_OK) {
                sendRequest(secondUsername, true);
                return true;
            }
            return false;
        }

        private void sendRequest(String username, boolean withSignature) throws IOException, SshException {
            final byte[] requestPayload = buildRequestPayload(username, withSignature);
            transport.postMessage(new AuthenticationMessage(username, "ssh-connection", "publickey") {
                @Override
                public boolean writeMessageIntoBuffer(ByteBuffer buf) {
                    super.writeMessageIntoBuffer(buf);
                    buf.put(requestPayload);
                    return true;
                }
            });
        }

        private byte[] buildRequestPayload(String username, boolean withSignature) throws IOException, SshException {
            byte[] keyBlob = keyPair.getPublicKey().getEncoded();
            String algorithm = keyPair.getPublicKey().getAlgorithm();

            try (ByteArrayWriter baw = new ByteArrayWriter()) {
                baw.writeBoolean(withSignature);
                baw.writeString(algorithm);
                baw.writeBinaryString(keyBlob);

                if (withSignature) {
                    byte[] dataToSign = buildSignatureData(username, algorithm, keyBlob);
                    byte[] signature = keyPair.sign(keyPair.getPublicKey(), keyPair.getPublicKey().getSigningAlgorithm(), dataToSign);
                    baw.writeBinaryString(signature);
                }

                return baw.toByteArray();
            }
        }

        private byte[] buildSignatureData(String username, String algorithm, byte[] keyBlob) throws IOException {
            try (ByteArrayWriter baw = new ByteArrayWriter()) {
                baw.writeBinaryString(transport.getSessionKey());
                baw.write(SSH_MSG_USERAUTH_REQUEST);
                baw.writeString(username);
                baw.writeString("ssh-connection");
                baw.writeString("publickey");
                baw.writeBoolean(true);
                baw.writeString(algorithm);
                baw.writeBinaryString(keyBlob);
                return baw.toByteArray();
            }
        }
    }

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

    @Test
    @DisplayName("Regression: public key probe user must not authenticate as different username")
    void publicKeyUsernameSwitchMustFail() throws Exception {
        try (SshClient client = SshClientBuilder.create()
            .withHostname("127.0.0.1")
            .withPort(SERVER.getPort())
            .withUsername("placeholder")
            .onConfigure(ctx -> ctx.setHostKeyVerification((host, pk) -> true))
            .build()) {

            ClientAuthenticator exploitAuthenticator = new UsernameSwitchPublicKeyAuthenticator(
                SERVER.getClientKeyPair(),
                SshServerExtension.TEST_USER,
                "otheruser");

            boolean authenticated = client.authenticate(exploitAuthenticator, 30000L);

            // Expected secure behavior: this must be false.
            // Current vulnerable behavior: this becomes true, so this test fails.
            assertFalse(authenticated, "Username switch during publickey auth must not authenticate.");
            assertFalse(client.isAuthenticated(), "Client must remain unauthenticated after username switch attempt.");
        }
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
