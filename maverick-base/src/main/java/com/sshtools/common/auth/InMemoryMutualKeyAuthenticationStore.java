package com.sshtools.common.auth;

import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public class InMemoryMutualKeyAuthenticationStore implements MutualKeyAuthenticatonStore {

	Map<String,SshKeyPair> privateKeys = new HashMap<>();
	Map<String,SshPublicKey> publicKeys = new HashMap<>();
	
	
	@Override
	public SshKeyPair getPrivateKey(String username) {
		return privateKeys.get(username);
	}
	
	@Override
	public SshPublicKey getPublicKey(String username) {
		return publicKeys.get(username);
	}

	public InMemoryMutualKeyAuthenticationStore addKey(String username, SshKeyPair privateKey, SshPublicKey publicKey) {
		privateKeys.put(username, privateKey);
		publicKeys.put(username, publicKey);
		return this;
	}
	
}
