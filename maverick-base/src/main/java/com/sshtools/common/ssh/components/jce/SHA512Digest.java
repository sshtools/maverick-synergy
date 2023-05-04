package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.DigestFactory;

/**
 * SHA-1 digest implementation.
 * @author Lee David Painter
 *
 */
public class SHA512Digest extends AbstractDigest {

	public static class SHA512DigestFactory implements DigestFactory<SHA512Digest> {
		@Override
		public SHA512Digest create() throws NoSuchAlgorithmException, IOException {
			return new SHA512Digest();
		}

		@Override
		public String[] getKeys() {
			return new String[] { JCEAlgorithms.JCE_SHA512, "SHA512" } ;
		}
	}

	public SHA512Digest() throws NoSuchAlgorithmException {
		super(JCEAlgorithms.JCE_SHA512);
	}

}
