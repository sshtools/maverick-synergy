package com.sshtools.common.ssh.components;

import com.sshtools.common.ssh.components.jce.OpenSshCertificate;

public class SshCertificate extends SshKeyPair {

	public static final int SSH_CERT_TYPE_USER = 1;
	public static final int SSH_CERT_TYPE_HOST = 2;
	
	OpenSshCertificate certificate;
	
	public SshCertificate(SshKeyPair pair, OpenSshCertificate certificate) {
		this.certificate = certificate;
		setPrivateKey(pair.getPrivateKey());
		setPublicKey(pair.getPublicKey());
	}
	
	public boolean isUserCertificate() {
		return certificate.isUserCertificate();
	}

	public boolean isHostCertificate() {
		return certificate.isHostCertificate();
	}

	public OpenSshCertificate getCertificate() {
		return certificate;
	}
}


