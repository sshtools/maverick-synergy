
package com.sshtools.common.ssh.components.jce;

import java.security.NoSuchAlgorithmException;

/**
 * SHA-1 digest implementation.
 * @author Lee David Painter
 *
 */
public class SHA512Digest extends AbstractDigest {

	public SHA512Digest() throws NoSuchAlgorithmException {
		super(JCEAlgorithms.JCE_SHA512);
	}

}
