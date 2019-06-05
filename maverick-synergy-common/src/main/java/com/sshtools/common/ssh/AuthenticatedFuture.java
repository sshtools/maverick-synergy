package com.sshtools.common.ssh;

public class AuthenticatedFuture extends AbstractRequestFuture {

	public void authenticated(boolean success) {
		done(success);
	}


}
