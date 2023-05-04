package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshCipherFactory;

/**
 * An implementation of the AES 128 bit cipher using a JCE provider.
 *
 * @author Lee David Painter
 */
public class AES128Cbc extends AbstractJCECipher {

	private static final String CIPHER = "aes128-cbc";

	public static class AES128CbcFactory implements SshCipherFactory<AES128Cbc> {

		@Override
		public AES128Cbc create() throws NoSuchAlgorithmException, IOException {
			return new AES128Cbc();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CIPHER };
		}

		@Override
		public boolean isEnabledByDefault() {
			return false;
		}
	}

	public AES128Cbc() throws IOException {
		super(JCEAlgorithms.JCE_AESCBCNOPADDING, "AES", 16, CIPHER, SecurityLevel.WEAK, 0);
	}

}
