
package com.sshtools.common.ssh.components.jce;


public class Ssh2EcdsaSha2Nist384PublicKey extends Ssh2EcdsaSha2NistPublicKey {

	public Ssh2EcdsaSha2Nist384PublicKey() {
		super("ecdsa-sha2-nistp384", JCEAlgorithms.JCE_SHA384WithECDSA, "secp384r1", "nistp384");
	}
	
	public byte[] getOid() {
		return new byte[] { 0x2B, (byte) 0x81, 0x04, 0x00, 0x22};
	}
}
