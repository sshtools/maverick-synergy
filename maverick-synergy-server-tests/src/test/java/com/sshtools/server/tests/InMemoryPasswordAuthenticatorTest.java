package com.sshtools.server.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sshtools.common.auth.PasswordChangeException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.InMemoryPasswordAuthenticator;

@DisplayName("InMemoryPasswordAuthenticator")
class InMemoryPasswordAuthenticatorTest {

    private InMemoryPasswordAuthenticator auth;
    private SshConnection mockCon;

    @BeforeEach
    void setUp() {
        auth = new InMemoryPasswordAuthenticator();
        mockCon = mock(SshConnection.class);
    }

    @Test
    @DisplayName("registered user with correct password authenticates successfully")
    void addUser_thenVerify_succeeds() throws PasswordChangeException, IOException {
        auth.addUser("alice", "correct".toCharArray());
        assertTrue(auth.verifyPassword(mockCon, "alice", "correct"));
    }

    @Test
    @DisplayName("correct user with wrong password is rejected")
    void wrongPassword_rejected() throws PasswordChangeException, IOException {
        auth.addUser("alice", "correct".toCharArray());
        assertFalse(auth.verifyPassword(mockCon, "alice", "wrong"));
    }

    @Test
    @DisplayName("unknown username is rejected")
    void unknownUser_rejected() throws PasswordChangeException, IOException {
        auth.addUser("alice", "correct".toCharArray());
        assertFalse(auth.verifyPassword(mockCon, "nobody", "correct"));
    }

    @Test
    @DisplayName("empty password stored – only matches if supplied password is also empty")
    void emptyPassword_onlyMatchesEmpty() throws PasswordChangeException, IOException {
        auth.addUser("alice", new char[0]);
        assertTrue(auth.verifyPassword(mockCon, "alice", ""));
        assertFalse(auth.verifyPassword(mockCon, "alice", "notempty"));
    }

    @Test
    @DisplayName("multiple users are isolated from each other")
    void multipleUsers_isolated() throws PasswordChangeException, IOException {
        auth.addUser("alice", "alicePass".toCharArray());
        auth.addUser("bob",   "bobPass".toCharArray());

        assertTrue(auth.verifyPassword(mockCon, "alice", "alicePass"));
        assertTrue(auth.verifyPassword(mockCon, "bob",   "bobPass"));
        // Alice's token should not authenticate Bob
        assertFalse(auth.verifyPassword(mockCon, "bob",   "alicePass"));
        assertFalse(auth.verifyPassword(mockCon, "alice", "bobPass"));
    }

    @Test
    @DisplayName("changePassword always returns false")
    void changePassword_returnsAlwaysFalse() throws PasswordChangeException, IOException {
        auth.addUser("alice", "old".toCharArray());
        assertFalse(auth.changePassword(mockCon, "alice", "old", "new"));
    }

    @Test
    @DisplayName("addUser returns this for fluent chaining")
    void addUser_returnsThis() {
        InMemoryPasswordAuthenticator returned = auth.addUser("alice", "pw".toCharArray());
        assertTrue(returned == auth, "addUser should return the same instance for fluent chaining");
    }
}
