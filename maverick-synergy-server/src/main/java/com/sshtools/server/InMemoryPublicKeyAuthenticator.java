
package com.sshtools.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.auth.AbstractPublicKeyAuthenticationProvider;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshPublicKey;

public class InMemoryPublicKeyAuthenticator extends AbstractPublicKeyAuthenticationProvider {

	Map<String,SshPublicKey> authorizedKeys = new HashMap<>();
	
	public InMemoryPublicKeyAuthenticator() {
	
	}

	public InMemoryPublicKeyAuthenticator addAuthorizedKey(String username, SshPublicKey key) {
		authorizedKeys.put(username, key);
		return this;
	}
	
	@Override
	public boolean isAuthorizedKey(SshPublicKey key, SshConnection con) throws IOException {
		return key.equals(authorizedKeys.get(con.getUsername()));
	}

}
