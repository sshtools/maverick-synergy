package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.SshPublicKeyFactory;

public class Ssh2EcdsaSha2Nist384PublicKey extends Ssh2EcdsaSha2NistPublicKey {
	private static final String CERT_TYPE = "ecdsa-sha2-nistp384";
	
	public static class Ssh2EcdsaSha2Nist384PublicKeyFactory implements SshPublicKeyFactory<Ssh2EcdsaSha2Nist384PublicKey> {

		@Override
		public Ssh2EcdsaSha2Nist384PublicKey create() throws NoSuchAlgorithmException, IOException {
			return new Ssh2EcdsaSha2Nist384PublicKey();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  CERT_TYPE };
		}
	}

	public Ssh2EcdsaSha2Nist384PublicKey() {
		super(CERT_TYPE, JCEAlgorithms.JCE_SHA384WithECDSA, "secp384r1", "nistp384");
	}
	
	public byte[] getOid() {
		return new byte[] { 0x2B, (byte) 0x81, 0x04, 0x00, 0x22};
	}
}
