
package com.sshtools.synergy.ssh;

import com.sshtools.common.ssh.AbstractRequestFuture;

public class AuthenticatedFuture extends AbstractRequestFuture {

	public void authenticated(boolean success) {
		done(success);
	}


}
