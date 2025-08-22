package com.sshtools.common.publickey;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshCertificate;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.util.ExpiringConcurrentHashMap;

public class EphemeralCertificateStore {

	final Map<String,SshCertificate> userCertificates;
	final Duration ttl;
	final String keyAlgorithm;
	final SshKeyPair caKey;
	public EphemeralCertificateStore(Duration ttl, String keyAlgorithm, SshKeyPair caKey) {
		this.ttl = ttl;
		this.keyAlgorithm = keyAlgorithm;
		this.caKey = caKey;
		this.userCertificates = new ExpiringConcurrentHashMap<String, SshCertificate>(ttl.toMillis());
	}
	
	public SshKeyPair getKey(String username) throws SshException, IOException {

		SshCertificate key = userCertificates.get(username);
		if(Objects.isNull(key)) {
			SshKeyPair tmp = SshKeyPairGenerator.generateKeyPair(keyAlgorithm);
			key = SshCertificateAuthority.generateUserCertificate(tmp, 
					System.currentTimeMillis(),
					username,
					1,
					caKey);
			userCertificates.put(username, key);
		}
		
		return key;
	}
}
