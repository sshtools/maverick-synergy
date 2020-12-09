package com.sshtools.common.ssh.components.jce;


public class OpenSshEcdsaSha2Nist521Certificate extends OpenSshEcdsaCertificate {

	public static final String CERT_TYPE = "ecdsa-sha2-nistp521-cert-v01@openssh.com";
	
	public OpenSshEcdsaSha2Nist521Certificate() {
		super("ecdsa-sha2-nistp521-cert-v01@openssh.com", JCEAlgorithms.JCE_SHA512WithECDSA, "secp521r1");
	}
	
	public int getPriority() {
		return super.getPriority() + 100;
	}
}
