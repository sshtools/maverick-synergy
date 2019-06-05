package com.sshtools.common.ssh.components.jce;

import java.security.NoSuchAlgorithmException;

/**
 * SHA-1 digest implementation.
 * @author Lee David Painter
 *
 */
public class SHA384Digest extends AbstractDigest {

	public SHA384Digest() throws NoSuchAlgorithmException {
		super(JCEAlgorithms.JCE_SHA384);
	}

}
