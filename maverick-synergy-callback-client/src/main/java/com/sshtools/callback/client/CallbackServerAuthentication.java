
package com.sshtools.callback.client;

import java.io.IOException;
import java.util.Set;

import com.sshtools.common.auth.AbstractPublicKeyAuthenticationProvider;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshPublicKey;

public class CallbackServerAuthentication extends AbstractPublicKeyAuthenticationProvider {

	Set<SshPublicKey> serverKeys;
	
	CallbackServerAuthentication(Set<SshPublicKey> serverKeys) throws IOException {
		
		this.serverKeys = serverKeys;
		
		if(serverKeys.isEmpty()) {
			throw new IOException("There are no keys available to authenticate the server!");
		}
	}
	
	@Override
	public boolean isAuthorizedKey(SshPublicKey key, SshConnection con) throws IOException {
		
		for(SshPublicKey serverKey : serverKeys) {
			if(key.equals(serverKey)) {
				return true;
			}
		}
		return false;
	}

}
