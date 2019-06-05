package com.sshtools.common.ssh.components.jce;

import java.io.IOException;

public class AES192Ctr extends AbstractJCECipher {

	public AES192Ctr() throws IOException {
		super(JCEAlgorithms.JCE_AESCTRNOPADDING, "AES", 24, "aes192-ctr");
	}

}
