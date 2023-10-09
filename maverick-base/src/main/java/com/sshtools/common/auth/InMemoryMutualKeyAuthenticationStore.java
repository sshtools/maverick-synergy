package com.sshtools.common.auth;

import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public class InMemoryMutualKeyAuthenticationStore implements MutualKeyAuthenticatonStore {

	Map<String,SshKeyPair> privateKeys = new HashMap<>();
	Map<String,SshPublicKey> publicKeys = new HashMap<>();
	
	
	@Override
	public SshKeyPair getPrivateKey(SshConnection con) {
		return privateKeys.get(con.getUsername());
	}
	
	@Override
	public SshPublicKey getPublicKey(SshConnection con) {
		return publicKeys.get(con.getUsername());
	}

	public InMemoryMutualKeyAuthenticationStore addKey(String username, SshKeyPair privateKey, SshPublicKey publicKey) {
		privateKeys.put(username, privateKey);
		publicKeys.put(username, publicKey);
		return this;
	}
	
}
