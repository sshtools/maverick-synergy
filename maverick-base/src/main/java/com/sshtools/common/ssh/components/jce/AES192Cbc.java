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
public class AES192Cbc extends AbstractJCECipher {

	private static final String CIPHER = "aes192-cbc";

	public static class AES192CbcFactory implements SshCipherFactory<AES192Cbc> {

		@Override
		public AES192Cbc create() throws NoSuchAlgorithmException, IOException {
			return new AES192Cbc();
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

	public AES192Cbc() throws IOException {
		super(JCEAlgorithms.JCE_AESCBCNOPADDING, "AES", 24, CIPHER, SecurityLevel.WEAK, 1);
	}

}
