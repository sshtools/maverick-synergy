package com.sshtools.common.ssh.components.jce;

import java.io.IOException;

public class AES256Ctr extends AbstractJCECipher {

	public AES256Ctr() throws IOException {
		super(JCEAlgorithms.JCE_AESCTRNOPADDING, "AES", 32, "aes256-ctr");
	}

}
