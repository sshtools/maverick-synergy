
package com.sshtools.common.ssh.components.jce;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshRsaPublicKey;

public class Ssh2RsaPublicKeySHA512 extends Ssh2RsaPublicKey {

	public Ssh2RsaPublicKeySHA512() {
		super();
	}

	public Ssh2RsaPublicKeySHA512(BigInteger modulus, BigInteger publicExponent)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		super(modulus, publicExponent);
	}

	public Ssh2RsaPublicKeySHA512(RSAPublicKey pubKey) {
		super(pubKey);
	}

	public Ssh2RsaPublicKeySHA512(SshRsaPublicKey publicKey) {
		this((RSAPublicKey)publicKey.getJCEPublicKey());
	}
	
	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.STRONG;
	}
	
	@Override
	public int getPriority() {
		return 2200;
	}

	@Override
	public String getSigningAlgorithm() {
		return "rsa-sha2-512";
	}

	@Override
	public String getAlgorithm() {
		return "rsa-sha2-512";
	}
	
	public String getEncodingAlgorithm() {
		return "ssh-rsa";
	}
}
