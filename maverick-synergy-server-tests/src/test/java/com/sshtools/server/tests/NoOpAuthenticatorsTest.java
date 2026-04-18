package com.sshtools.server.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sshtools.common.auth.PasswordChangeException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.server.NoOpPasswordAuthenticator;
import com.sshtools.server.NoOpPublicKeyAuthenticator;

@DisplayName("NoOp authenticators always reject")
class NoOpAuthenticatorsTest {

    @Test
    @DisplayName("NoOpPasswordAuthenticator rejects any username/password combination")
    void noOpPassword_alwaysRejects() throws PasswordChangeException, IOException {
        NoOpPasswordAuthenticator auth = new NoOpPasswordAuthenticator();
        SshConnection con = mock(SshConnection.class);

        assertFalse(auth.verifyPassword(con, "admin",   "admin"));
        assertFalse(auth.verifyPassword(con, "root",    ""));
        assertFalse(auth.verifyPassword(con, "anyuser", "anypass"));
    }

    @Test
    @DisplayName("NoOpPasswordAuthenticator.changePassword always returns false")
    void noOpPassword_changePassword_alwaysRejects() throws PasswordChangeException, IOException {
        NoOpPasswordAuthenticator auth = new NoOpPasswordAuthenticator();
        SshConnection con = mock(SshConnection.class);

        assertFalse(auth.changePassword(con, "alice", "old", "new"));
    }

    @Test
    @DisplayName("NoOpPublicKeyAuthenticator.isAuthorizedKey always returns false")
    void noOpPublicKey_isAuthorizedKey_alwaysRejects() throws IOException {
        NoOpPublicKeyAuthenticator auth = new NoOpPublicKeyAuthenticator();
        SshConnection con = mock(SshConnection.class);
        SshPublicKey key  = mock(SshPublicKey.class);

        assertFalse(auth.isAuthorizedKey(key, con));
    }

    @Test
    @DisplayName("NoOpPublicKeyAuthenticator.checkKey always returns false")
    void noOpPublicKey_checkKey_alwaysRejects() throws IOException {
        NoOpPublicKeyAuthenticator auth = new NoOpPublicKeyAuthenticator();
        SshConnection con = mock(SshConnection.class);
        SshPublicKey key  = mock(SshPublicKey.class);

        assertFalse(auth.checkKey(key, con));
    }
}
