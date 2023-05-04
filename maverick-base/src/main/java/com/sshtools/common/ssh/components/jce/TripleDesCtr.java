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
public class TripleDesCtr extends AbstractJCECipher {

	private static final String CIPHER = "3des-ctr";

	public static class TripleDesCtrFactory implements SshCipherFactory<TripleDesCtr> {

		@Override
		public TripleDesCtr create() throws NoSuchAlgorithmException, IOException {
			return new TripleDesCtr();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CIPHER };
		}
	}

	public TripleDesCtr() throws IOException {
		super(JCEAlgorithms.JCE_3DESCTRNOPADDING, "DESede", 24, CIPHER, SecurityLevel.WEAK, 0);
	}

}
