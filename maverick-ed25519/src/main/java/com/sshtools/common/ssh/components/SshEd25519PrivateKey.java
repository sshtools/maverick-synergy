package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;

public class SshEd25519PrivateKey implements SshPrivateKey {

	EdDSAPrivateKey key;
	byte[] sk;
	public SshEd25519PrivateKey(byte[] sk, byte[] pk) {
		this.sk = Arrays.copyOfRange(sk, 0, 32);
		EdDSAPrivateKeySpec spec = new EdDSAPrivateKeySpec(this.sk, 
				EdDSANamedCurveTable.getByName("Ed25519"));
		key = new EdDSAPrivateKey(spec);
	}
	
	public SshEd25519PrivateKey(PrivateKey prv) {
		if(!(prv instanceof EdDSAPrivateKey)) {
			throw new IllegalArgumentException("Invalid PrivateKey type passed to SshEd25519PrivateKey");
		}
		
		key = (EdDSAPrivateKey) prv;
	}

	@Override
	public byte[] sign(byte[] data) throws IOException {
		return sign(data, getAlgorithm());
	}
	
	@Override
	public byte[] sign(byte[] data, String signingAlgorithm) throws IOException {
		try {
			Signature sgr = new EdDSAEngine();
			sgr.initSign(key);
			sgr.update(data);
			return sgr.sign();
		} catch (InvalidKeyException | SignatureException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	@Override
	public String getAlgorithm() {
		return SshEd25519PublicKey.ALGORITHM_NAME;
	}

	@Override
	public PrivateKey getJCEPrivateKey() {
		return key;
	}

	public byte[] getSeed() {
		return key.getSeed();
	}

	@Override
	public int hashCode() {
		return new String(Utils.bytesToHex(getSeed())).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof SshEd25519PrivateKey)) {
			return false;
		}
		return getJCEPrivateKey().equals(((SshEd25519PrivateKey)obj).getJCEPrivateKey());
	}

	public byte[] getH() {
		return key.geta();
	}
}
