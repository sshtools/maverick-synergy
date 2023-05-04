package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.DigestFactory;

/**
 * SHA-1 digest implementation.
 * @author Lee David Painter
 *
 */
public class SHA256Digest extends AbstractDigest {

	public static class SHA256DigestFactory implements DigestFactory<SHA256Digest> {
		public SHA256Digest create() throws NoSuchAlgorithmException, IOException {
			return new SHA256Digest();
		}

		@Override
		public String[] getKeys() {
			return new String[] { JCEAlgorithms.JCE_SHA256, "SHA256" };
		}
	}

	public SHA256Digest() throws NoSuchAlgorithmException {
		super(JCEAlgorithms.JCE_SHA256);
	}

}
