package com.sshtools.client;

import java.io.IOException;

import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.common.util.ByteArrayReader;

/**
 * A simple base class for implementing non-interactive authentication methods. Use when the 
 * authentication method consists of a single message sent to the server. 
 */
public abstract class SimpleClientAuthenticator extends AbstractRequestFuture implements ClientAuthenticator {

	@Override
	public boolean processMessage(ByteArrayReader msg) throws IOException {
		return false;
	}

	public void success() {
		done(true);
	}
	
	public void failure() {
		done(false);
	}
}
