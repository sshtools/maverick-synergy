package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshCipherFactory;

/**
 * An implementation of the Blowfish cipher using a JCE provider. If you have
 * enabled JCE usage there is no need to configure this separately.
 * 
 * @author Lee David Painter
 */
public class BlowfishCbc extends AbstractJCECipher {

	private static final String CIPHER = "blowfish-cbc";

	public static class BlowfishCbcFactory implements SshCipherFactory<BlowfishCbc> {

		@Override
		public BlowfishCbc create() throws NoSuchAlgorithmException, IOException {
			return new BlowfishCbc();
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

	public BlowfishCbc() throws IOException {
		super(JCEAlgorithms.JCE_BLOWFISHCBCNOPADDING, "Blowfish", 16, CIPHER, SecurityLevel.WEAK, 0);
	}

}
