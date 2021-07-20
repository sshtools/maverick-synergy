
package com.sshtools.common.ssh.components.jce;


public class Ssh2EcdsaSha2Nist521PublicKey extends Ssh2EcdsaSha2NistPublicKey {

	public Ssh2EcdsaSha2Nist521PublicKey() {
		super("ecdsa-sha2-nistp521", JCEAlgorithms.JCE_SHA512WithECDSA, "secp521r1", "nistp521");
	}
	
	public byte[] getOid() {
		return new byte[] { 0x2B, (byte) 0x81, 0x04, 0x00, 0x23};
	}
}
