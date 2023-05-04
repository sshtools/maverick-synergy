package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.DigestFactory;

/**
 * MD5 digest implementation.
 * @author Lee David Painter
 *
 */
public class MD5Digest extends AbstractDigest {

	public static class MD5DigestFactory implements DigestFactory<MD5Digest> {
		public MD5Digest create() throws NoSuchAlgorithmException, IOException {
			return new MD5Digest();
		}

		@Override
		public String[] getKeys() {
			return new String[] { JCEAlgorithms.JCE_MD5 };
		}
	}

	public MD5Digest() throws NoSuchAlgorithmException {
		super(JCEAlgorithms.JCE_MD5);
	}

}
