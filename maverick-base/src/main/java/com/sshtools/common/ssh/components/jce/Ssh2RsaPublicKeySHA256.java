package com.sshtools.common.ssh.components.jce;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import com.sshtools.common.ssh.components.SshRsaPublicKey;

public class Ssh2RsaPublicKeySHA256 extends Ssh2RsaPublicKey {

	public Ssh2RsaPublicKeySHA256() {
		super();
	}

	public Ssh2RsaPublicKeySHA256(BigInteger modulus, BigInteger publicExponent)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		super(modulus, publicExponent);
	}

	public Ssh2RsaPublicKeySHA256(RSAPublicKey pubKey) {
		super(pubKey);
	}

	public Ssh2RsaPublicKeySHA256(SshRsaPublicKey publicKey) {
		this((RSAPublicKey)publicKey.getJCEPublicKey());
	}

	@Override
	public String getSigningAlgorithm() {
		return "rsa-sha2-256";
	}
	
	@Override
	public String getAlgorithm() {
		return "rsa-sha2-256";
	}
	
	public String getEncodingAlgorithm() {
		return "ssh-rsa";
	}
}
