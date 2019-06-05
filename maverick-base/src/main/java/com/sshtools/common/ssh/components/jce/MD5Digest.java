package com.sshtools.common.ssh.components.jce;

import java.security.NoSuchAlgorithmException;

/**
 * MD5 digest implementation.
 * @author Lee David Painter
 *
 */
public class MD5Digest extends AbstractDigest {

	public MD5Digest() throws NoSuchAlgorithmException {
		super(JCEAlgorithms.JCE_MD5);
	}

}
