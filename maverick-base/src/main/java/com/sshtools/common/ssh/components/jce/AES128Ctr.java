package com.sshtools.common.ssh.components.jce;

import java.io.IOException;

public class AES128Ctr extends AbstractJCECipher {

	public AES128Ctr() throws IOException {
		super(JCEAlgorithms.JCE_AESCTRNOPADDING, "AES", 16, "aes128-ctr");
	}
}
