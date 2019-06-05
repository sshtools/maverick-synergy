package com.sshtools.common.ssh.components;

import com.sshtools.common.ssh.SshException;

/**
 * This interface should be implemented by all secure random number generator
 * implementations.
 * @author Lee David Painter
 *
 */
public interface SshSecureRandomGenerator {

	void nextBytes(byte[] bytes);
	
	void nextBytes(byte[] bytes, int off, int len) throws SshException;
	
	int nextInt();
}