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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.common.ssh.SshException;

/**
 * Integration tests verifying that a client can connect to, authenticate
 * against, and disconnect from the embedded test SSH server.
 */
@DisplayName("Connection lifecycle")
class ConnectionIT extends AbstractSshIntegrationTest {

    @Test
    @DisplayName("Password authentication establishes an authenticated connection")
    void connectWithPasswordSucceeds() throws IOException, SshException {
        try (SshClient client = connectWithPassword()) {
            assertTrue(client.isConnected(), "client should report connected after build()");
            assertTrue(client.isAuthenticated(), "client should be authenticated after build()");
        }
    }

    @Test
    @DisplayName("Public-key authentication establishes an authenticated connection")
    void connectWithPublicKeySucceeds() throws IOException, SshException {
        try (SshClient client = connectWithPublicKey()) {
            assertTrue(client.isConnected());
            assertTrue(client.isAuthenticated());
        }
    }

    @Test
    @DisplayName("Disconnecting the client transitions it to disconnected state")
    void disconnectResultsInNotConnected() throws IOException, SshException {
        SshClient client = connectWithPassword();
        assertTrue(client.isConnected());
        client.disconnect();
        assertFalse(client.isConnected(), "client should not report connected after explicit disconnect");
    }

    @Test
    @DisplayName("Wrong password causes build() to throw IOException")
    void wrongPasswordThrows() {
        assertThrows(IOException.class, () ->
            SshClientBuilder.create()
                .withHostname("127.0.0.1")
                .withPort(SERVER.getPort())
                .withUsername(SshServerExtension.TEST_USER)
                .withPassword("totallyWrong!")
                .onConfigure(ctx -> ctx.setHostKeyVerification((host, pk) -> true))
                .build()
        );
    }

    @Test
    @DisplayName("Connecting to a port with no listener throws IOException")
    void unreachableHostThrows() {
        // Port 1 is reserved and should be refused or time out quickly
        assertThrows(IOException.class, () ->
            SshClientBuilder.create()
                .withHostname("127.0.0.1")
                .withPort(1)
                .withUsername(SshServerExtension.TEST_USER)
                .withPassword(SshServerExtension.TEST_PASSWORD)
                .withConnectTimeout(2000L)
                .onConfigure(ctx -> ctx.setHostKeyVerification((host, pk) -> true))
                .build()
        );
    }
}
