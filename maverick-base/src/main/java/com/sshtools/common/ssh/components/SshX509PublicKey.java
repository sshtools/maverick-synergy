package com.sshtools.common.ssh.components;

import java.security.cert.Certificate;

public interface SshX509PublicKey {

	Certificate getCertificate();
	
	Certificate[] getCertificateChain();
	

}
