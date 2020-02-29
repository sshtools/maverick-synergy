package com.sshtools.common.auth;

import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public class MutualKeyAuthenticationStore {

	Map<String,SshKeyPair> privateKeys = new HashMap<>();
	Map<String,SshPublicKey> publicKeys = new HashMap<>();
	
	
	public SshKeyPair getPrivateKey(String username) {
		return privateKeys.get(username);
	}
	
	public SshPublicKey getPublicKey(String username) {
		return publicKeys.get(username);
	}

	public MutualKeyAuthenticationStore addKey(String username, SshKeyPair privateKey, SshPublicKey publicKey) {
		privateKeys.put(username, privateKey);
		publicKeys.put(username, publicKey);
		return this;
	}
	
}
