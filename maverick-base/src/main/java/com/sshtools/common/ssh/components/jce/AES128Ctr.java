package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshCipherFactory;

public class AES128Ctr extends AbstractJCECipher {
	
	private static final String CIPHER = "aes128-ctr";

	public static class AES128CtrFactory implements SshCipherFactory<AES128Ctr> {

		@Override
		public AES128Ctr create() throws NoSuchAlgorithmException, IOException {
			return new AES128Ctr();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  CIPHER };
		}
	}

	public AES128Ctr() throws IOException {
		super(JCEAlgorithms.JCE_AESCTRNOPADDING, "AES", 16, CIPHER, SecurityLevel.STRONG, 2000);
	}
}
