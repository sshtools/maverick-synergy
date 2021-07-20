
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;

import com.sshtools.common.ssh.SecurityLevel;

public class AES192Ctr extends AbstractJCECipher {

	public AES192Ctr() throws IOException {
		super(JCEAlgorithms.JCE_AESCTRNOPADDING, "AES", 24, "aes192-ctr", SecurityLevel.STRONG, 2001);
	}

}
