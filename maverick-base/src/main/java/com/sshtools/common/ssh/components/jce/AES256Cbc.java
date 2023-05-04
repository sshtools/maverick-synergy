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
public class AES256Cbc extends AbstractJCECipher {

	private static final String CIPHER = "aes256-cbc";

	public static class AES256CbcFactory implements SshCipherFactory<AES256Cbc> {

		@Override
		public AES256Cbc create() throws NoSuchAlgorithmException, IOException {
			return new AES256Cbc();
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

	public AES256Cbc() throws IOException {
		super(JCEAlgorithms.JCE_AESCBCNOPADDING, "AES", 32, CIPHER, SecurityLevel.WEAK, 3);
	}

}
