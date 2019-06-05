package com.sshtools.common.ssh.components.jce;


public class Ssh2EcdsaSha2Nist256PublicKey extends Ssh2EcdsaSha2NistPublicKey {

	public Ssh2EcdsaSha2Nist256PublicKey() {
		super("ecdsa-sha2-nistp256", JCEAlgorithms.JCE_SHA256WithECDSA, "secp256r1", "nistp256");
	}
	
	public byte[] getOid() {
		return new byte[] { 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x03, 0x01, 0x07};
	}
}
