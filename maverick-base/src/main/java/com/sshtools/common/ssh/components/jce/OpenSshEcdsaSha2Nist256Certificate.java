package com.sshtools.common.ssh.components.jce;


public class OpenSshEcdsaSha2Nist256Certificate extends OpenSshEcdsaCertificate {

	public static final String CERT_TYPE = "ecdsa-sha2-nistp256-cert-v01@openssh.com";
	
	public OpenSshEcdsaSha2Nist256Certificate() {
		super(CERT_TYPE, JCEAlgorithms.JCE_SHA256WithECDSA, "secp256r1");
	}
	
	public int getPriority() {
		return super.getPriority() + 100;
	}
}
