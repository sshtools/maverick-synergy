package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshCipherFactory;

public class AES192Ctr extends AbstractJCECipher {

	private static final String CIPHER = "aes192-ctr";

	public static class AES192CtrFactory implements SshCipherFactory<AES192Ctr> {

		@Override
		public AES192Ctr create() throws NoSuchAlgorithmException, IOException {
			return new AES192Ctr();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CIPHER };
		}
	}

	public AES192Ctr() throws IOException {
		super(JCEAlgorithms.JCE_AESCTRNOPADDING, "AES", 24, CIPHER, SecurityLevel.STRONG, 2001);
	}

}
