package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.DigestFactory;

/**
 * SHA-1 digest implementation.
 * @author Lee David Painter
 *
 */
public class SHA384Digest extends AbstractDigest {

	public static class SHA384DigestFactory implements DigestFactory<SHA384Digest> {
		@Override
		public SHA384Digest create() throws NoSuchAlgorithmException, IOException {
			return new SHA384Digest();
		}

		@Override
		public String[] getKeys() {
			return new String[] { JCEAlgorithms.JCE_SHA384, "SHA384" } ;
		}
	}

	public SHA384Digest() throws NoSuchAlgorithmException {
		super(JCEAlgorithms.JCE_SHA384);
	}

}
