package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshCipherFactory;

/**
 * An implementation of the 3DES cipher using a JCE provider.
 * 
 * @author Lee David Painter
 */
public class TripleDesCbc extends AbstractJCECipher {

	private static final String CIPHER = "3des-cbc";

	public static class TripleDesCbcFactory implements SshCipherFactory<TripleDesCbc> {

		@Override
		public TripleDesCbc create() throws NoSuchAlgorithmException, IOException {
			return new TripleDesCbc();
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

	public TripleDesCbc() throws IOException {
		super(JCEAlgorithms.JCE_3DESCBCNOPADDING, "DESede", 24, CIPHER, SecurityLevel.WEAK, -1);
	}

}
