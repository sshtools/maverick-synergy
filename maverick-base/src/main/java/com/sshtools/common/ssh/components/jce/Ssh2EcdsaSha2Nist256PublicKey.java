package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.SshPublicKeyFactory;

public class Ssh2EcdsaSha2Nist256PublicKey extends Ssh2EcdsaSha2NistPublicKey {
	private static final String CERT_TYPE = "ecdsa-sha2-nistp256";
	
	public static class Ssh2EcdsaSha2Nist256PublicKeyFactory implements SshPublicKeyFactory<Ssh2EcdsaSha2Nist256PublicKey> {

		@Override
		public Ssh2EcdsaSha2Nist256PublicKey create() throws NoSuchAlgorithmException, IOException {
			return new Ssh2EcdsaSha2Nist256PublicKey();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  CERT_TYPE };
		}
	}

	public Ssh2EcdsaSha2Nist256PublicKey() {
		super(CERT_TYPE, JCEAlgorithms.JCE_SHA256WithECDSA, "secp256r1", "nistp256");
	}
	
	public byte[] getOid() {
		return new byte[] { 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x03, 0x01, 0x07};
	}
}
