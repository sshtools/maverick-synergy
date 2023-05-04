package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.DigestFactory;

/**
 * SHA-1 digest implementation.
 * @author Lee David Painter
 *
 */
public class SHA1Digest extends AbstractDigest {

	public static class SHA1DigestFactory implements DigestFactory<SHA1Digest> {
		public SHA1Digest create() throws NoSuchAlgorithmException, IOException {
			return new SHA1Digest();
		}

		@Override
		public String[] getKeys() {
			return new String[] { JCEAlgorithms.JCE_SHA1, "SHA1" };
		}
	}

	public SHA1Digest() throws NoSuchAlgorithmException {
		super(JCEAlgorithms.JCE_SHA1);
	}

}
