package com.sshtools.synergy.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.common.ssh.RequestFuture;
import com.sshtools.common.ssh.SshException;

/**
 * Integration tests for the SSH session-channel lifecycle: opening channels,
 * verifying their open/closed state, and ensuring that multiple channels can
 * be opened on a single connection.
 *
 * <p>Note: the in-process test server does not register a shell or exec command
 * factory, so these tests focus on channel lifecycle rather than exec output.
 */
@DisplayName("Session channel lifecycle")
class SessionChannelIT extends AbstractSshIntegrationTest {

    @Test
    @DisplayName("openSessionChannel() returns a non-null channel")
    void openSessionChannel_returnsNonNull() throws IOException, SshException {
        try (SshClient ssh = connectWithPassword()) {
            SessionChannelNG channel = ssh.openSessionChannel();
            assertNotNull(channel, "openSessionChannel() must not return null");
            channel.close();
        }
    }

    @Test
    @DisplayName("Newly opened session channel reports isOpen() == true")
    void newlyOpenedChannel_isOpen() throws IOException, SshException {
        try (SshClient ssh = connectWithPassword()) {
            SessionChannelNG channel = ssh.openSessionChannel();
            assertNotNull(channel);
            assertFalse(channel.isClosed(), "channel should not be closed right after open");
            channel.close();
        }
    }

    @Test
    @DisplayName("Connection is authenticated before opening session channel")
    void connectionIsAuthenticated_beforeChannelOpen() throws IOException, SshException {
        try (SshClient ssh = connectWithPassword()) {
            assertTrue(ssh.isAuthenticated(),
                    "client must be authenticated before a session channel can be opened");
            SessionChannelNG channel = ssh.openSessionChannel();
            assertNotNull(channel);
            channel.close();
        }
    }

    @Test
    @DisplayName("Two session channels can be opened on the same authenticated connection")
    void twoChannels_sameSshConnection() throws IOException, SshException {
        try (SshClient ssh = connectWithPassword()) {
            SessionChannelNG ch1 = ssh.openSessionChannel();
            SessionChannelNG ch2 = ssh.openSessionChannel();
            assertNotNull(ch1, "first channel must not be null");
            assertNotNull(ch2, "second channel must not be null");
            assertFalse(ch1.isClosed(), "first channel should be open");
            assertFalse(ch2.isClosed(), "second channel should be open");
            ch1.close();
            ch2.close();
        }
    }

    @Test
    @DisplayName("Session channel opened with public-key auth is non-null and open")
    void openSessionChannel_publicKeyAuth() throws IOException, SshException {
        try (SshClient ssh = connectWithPublicKey()) {
            SessionChannelNG channel = ssh.openSessionChannel();
            assertNotNull(channel, "channel opened via public-key auth must not be null");
            assertFalse(channel.isClosed(), "channel opened via public-key auth must be open");
            channel.close();
        }
    }

    @Test
    @DisplayName("allocatePseudoTerminal() returns a non-null RequestFuture without throwing")
    void allocatePseudoTerminal_returnsRequestFuture() throws IOException, SshException {
        try (SshClient ssh = connectWithPassword()) {
            SessionChannelNG channel = ssh.openSessionChannel();
            assertNotNull(channel);

            RequestFuture future = channel.allocatePseudoTerminal("vt100", 80, 24);
            assertNotNull(future, "allocatePseudoTerminal() must return a non-null RequestFuture");

            // Wait up to 3 seconds for the server to respond (it may reject
            // the request without a command factory, but it must not throw).
            future.waitFor(3000);
            assertTrue(future.isDone(), "PTY request future must be done after waiting");

            channel.close();
        }
    }

    @Test
    @DisplayName("setEnvironmentVariable() returns a non-null RequestFuture without throwing")
    void setEnvironmentVariable_returnsRequestFuture() throws IOException, SshException {
        try (SshClient ssh = connectWithPassword()) {
            SessionChannelNG channel = ssh.openSessionChannel();
            assertNotNull(channel);

            RequestFuture future = channel.setEnvironmentVariable("TERM", "vt100");
            assertNotNull(future, "setEnvironmentVariable() must return a non-null RequestFuture");

            future.waitFor(3000);
            assertTrue(future.isDone(), "env-var request future must be done after waiting");

            channel.close();
        }
    }

    @Test
    @DisplayName("Closed channel reports isClosed() == true")
    void closedChannel_isClosedTrue() throws IOException, SshException {
        try (SshClient ssh = connectWithPassword()) {
            SessionChannelNG channel = ssh.openSessionChannel();
            assertNotNull(channel);
            assertFalse(channel.isClosed(), "channel must be open before close()");

            channel.close();

            // After explicit close the channel should report itself as closed.
            assertTrue(channel.isClosed(), "channel must be closed after close()");
        }
    }
}
