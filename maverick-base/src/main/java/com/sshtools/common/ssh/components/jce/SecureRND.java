
package com.sshtools.common.ssh.components.jce;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshSecureRandomGenerator;

/**
 * Secure random number generator implementation for JCE provider. 
 */
public class SecureRND implements SshSecureRandomGenerator {

	SecureRandom rnd;
	
	public SecureRND() throws NoSuchAlgorithmException {
		rnd = JCEProvider.getSecureRandom();
	}
	
	public void nextBytes(byte[] bytes) {
		rnd.nextBytes(bytes);
	}

	public void nextBytes(byte[] bytes, int off, int len) throws SshException {
		
		try {
			byte[] tmp = new byte[len];
			rnd.nextBytes(tmp);
			System.arraycopy(tmp, 0, bytes, off, len);
		} catch(ArrayIndexOutOfBoundsException ex){
			throw new SshException("ArrayIndexOutOfBoundsException: Index " + off + " on actual array length " + bytes.length + " with len=" + len, SshException.INTERNAL_ERROR);
		}
	}

	public int nextInt() {
		return rnd.nextInt();
	}

}
