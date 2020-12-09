package com.sshtools.common.ssh.components.jce;


public class OpenSshEcdsaSha2Nist384Certificate extends OpenSshEcdsaCertificate {

	public static final String CERT_TYPE = "ecdsa-sha2-nistp384-cert-v01@openssh.com";
	
	public OpenSshEcdsaSha2Nist384Certificate() {
		super("ecdsa-sha2-nistp384-cert-v01@openssh.com", JCEAlgorithms.JCE_SHA384WithECDSA, "secp384r1");
	}
	
	public int getPriority() {
		return super.getPriority() + 100;
	}
}
