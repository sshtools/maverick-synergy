package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.SshPublicKeyFactory;

public class OpenSshEcdsaSha2Nist521Certificate extends OpenSshEcdsaCertificate {

	public static final String CERT_TYPE = "ecdsa-sha2-nistp521-cert-v01@openssh.com";
	
	public static class OpenSshEcdsaSha2Nist521CertificateFactory implements SshPublicKeyFactory<OpenSshEcdsaSha2Nist521Certificate> {
		@Override
		public OpenSshEcdsaSha2Nist521Certificate create() throws NoSuchAlgorithmException, IOException {
			return new OpenSshEcdsaSha2Nist521Certificate();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  CERT_TYPE };
		}
	}
	
	public OpenSshEcdsaSha2Nist521Certificate() {
		super(CERT_TYPE, JCEAlgorithms.JCE_SHA512WithECDSA, "secp521r1");
	}
	
	public int getPriority() {
		return super.getPriority() + 100;
	}
}
