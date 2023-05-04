package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.SshPublicKeyFactory;

public class OpenSshEcdsaSha2Nist384Certificate extends OpenSshEcdsaCertificate {

	public static final String CERT_TYPE = "ecdsa-sha2-nistp384-cert-v01@openssh.com";
	
	public static class OpenSshEcdsaSha2Nist384CertificateFactory implements SshPublicKeyFactory<OpenSshEcdsaSha2Nist384Certificate> {
		@Override
		public OpenSshEcdsaSha2Nist384Certificate create() throws NoSuchAlgorithmException, IOException {
			return new OpenSshEcdsaSha2Nist384Certificate();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  CERT_TYPE };
		}
	}
	
	public OpenSshEcdsaSha2Nist384Certificate() {
		super(CERT_TYPE, JCEAlgorithms.JCE_SHA384WithECDSA, "secp384r1");
	}
	
	public int getPriority() {
		return super.getPriority() + 100;
	}
}
