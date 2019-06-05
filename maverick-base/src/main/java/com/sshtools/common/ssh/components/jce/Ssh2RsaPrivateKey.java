package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;

import com.sshtools.common.ssh.components.SshRsaPrivateKey;

/**
 * RSA private key implementation for the SSH2 protocol.
 *
 * @author Lee David Painter
 *
 */
public class Ssh2RsaPrivateKey extends Ssh2BaseRsaPrivateKey implements SshRsaPrivateKey {

	public Ssh2RsaPrivateKey(RSAPrivateKey prv) {
		super(prv);
	}

	public Ssh2RsaPrivateKey(BigInteger modulus, BigInteger privateExponent)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		super(null);
		KeyFactory keyFactory = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA) == null ? KeyFactory
				.getInstance(JCEAlgorithms.JCE_RSA) : KeyFactory.getInstance(JCEAlgorithms.JCE_RSA,
				JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA));
		RSAPrivateKeySpec spec = new RSAPrivateKeySpec(modulus, privateExponent);
		this.prv = (RSAPrivateKey) keyFactory.generatePrivate(spec);

	}

	public byte[] sign(byte[] data) throws IOException {
		return sign(data, getAlgorithm());
	}
	
	public byte[] sign(byte[] data, String signingAlgorithm) throws IOException {
		return super.doSign(data, signingAlgorithm);
	}

	public String getAlgorithm() {
		return "ssh-rsa";
	}

	public BigInteger getModulus() {
		return ((RSAPrivateKey)prv).getModulus();
	}

	public BigInteger getPrivateExponent() {
		return ((RSAPrivateKey)prv).getPrivateExponent();
	}

	@Override
	public PrivateKey getJCEPrivateKey() {
		return prv;
	}

	@Override
	public int hashCode() {
		return prv.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		}
		if(obj instanceof Ssh2RsaPrivateKey) {
			Ssh2RsaPrivateKey other = (Ssh2RsaPrivateKey)obj;
			if(other.prv!=null) {
				return other.prv.equals(prv);
			}
		}
		return false;
	}
}
