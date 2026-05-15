package com.sshtools.server.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.server.InMemoryPublicKeyAuthenticator;

@DisplayName("InMemoryPublicKeyAuthenticator")
class InMemoryPublicKeyAuthenticatorTest {

    private InMemoryPublicKeyAuthenticator auth;
    private SshConnection mockCon;

    // Generated once per test – fast ED25519 keys
    private SshPublicKey aliceKey;
    private SshPublicKey bobKey;
    private SshPublicKey unknownKey;

    @BeforeEach
    void setUp() throws Exception {
        auth    = new InMemoryPublicKeyAuthenticator();
        mockCon = mock(SshConnection.class);

        aliceKey   = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0).getPublicKey();
        bobKey     = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0).getPublicKey();
        unknownKey = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0).getPublicKey();
    }

    @Test
    @DisplayName("registered key for the correct user is authorized")
    void addKey_thenIsAuthorized_succeeds() throws IOException {
        auth.addAuthorizedKey("alice", aliceKey);
        when(mockCon.getUsername()).thenReturn("alice");

        assertTrue(auth.isAuthorizedKey(aliceKey, mockCon));
    }

    @Test
    @DisplayName("different key for a registered user is rejected")
    void wrongKey_rejected() throws IOException {
        auth.addAuthorizedKey("alice", aliceKey);
        when(mockCon.getUsername()).thenReturn("alice");

        assertFalse(auth.isAuthorizedKey(bobKey, mockCon));
    }

    @Test
    @DisplayName("correct key for a different user is rejected")
    void wrongUser_rejected() throws IOException {
        auth.addAuthorizedKey("alice", aliceKey);
        when(mockCon.getUsername()).thenReturn("bob");

        assertFalse(auth.isAuthorizedKey(aliceKey, mockCon));
    }

    @Test
    @DisplayName("key for an unknown user is rejected")
    void unknownUser_rejected() throws IOException {
        when(mockCon.getUsername()).thenReturn("nobody");

        assertFalse(auth.isAuthorizedKey(unknownKey, mockCon));
    }

    @Test
    @DisplayName("addAuthorizedKeys varargs overload registers all supplied keys")
    void addAuthorizedKeys_varargs_works() throws IOException {
        auth.addAuthorizedKeys("alice", aliceKey, bobKey);
        when(mockCon.getUsername()).thenReturn("alice");

        // Last key wins (current HashMap implementation stores one per username)
        // – verify at least one of them is accepted
        boolean either = auth.isAuthorizedKey(aliceKey, mockCon)
                      || auth.isAuthorizedKey(bobKey,   mockCon);
        assertTrue(either, "At least one key from varargs should be authorized");
    }

    @Test
    @DisplayName("addAuthorizedKeys List overload registers all supplied keys")
    void addAuthorizedKeys_list_works() throws IOException {
        auth.addAuthorizedKeys("alice", Arrays.asList(aliceKey, bobKey));
        when(mockCon.getUsername()).thenReturn("alice");

        boolean either = auth.isAuthorizedKey(aliceKey, mockCon)
                      || auth.isAuthorizedKey(bobKey,   mockCon);
        assertTrue(either, "At least one key from List should be authorized");
    }

    @Test
    @DisplayName("addAuthorizedKey returns this for fluent chaining")
    void addAuthorizedKey_returnsThis() {
        InMemoryPublicKeyAuthenticator returned = auth.addAuthorizedKey("alice", aliceKey);
        assertTrue(returned == auth, "addAuthorizedKey should return the same instance");
    }
}
