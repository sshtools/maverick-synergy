package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshCipherFactory;

public class AES256Ctr extends AbstractJCECipher {

	private static final String CIPHER = "aes256-ctr";

	public static class AES256CtrFactory implements SshCipherFactory<AES256Ctr> {

		@Override
		public AES256Ctr create() throws NoSuchAlgorithmException, IOException {
			return new AES256Ctr();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CIPHER };
		}
	}

	public AES256Ctr() throws IOException {
		super(JCEAlgorithms.JCE_AESCTRNOPADDING, "AES", 32, CIPHER, SecurityLevel.STRONG, 2002);
	}

}
