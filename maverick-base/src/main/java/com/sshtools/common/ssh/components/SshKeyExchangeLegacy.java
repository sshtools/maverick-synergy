package com.sshtools.common.ssh.components;

import java.io.IOException;

import com.sshtools.common.ssh.SshException;

/**
 * Base interface for SSH2 key exchange implementations. 
 * @author Lee David Painter
 *
 */
public interface SshKeyExchangeLegacy extends SshComponent {

	String getHashAlgorithm();
	
	void test() throws IOException, SshException;

	String getProvider();
}
