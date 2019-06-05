package com.sshtools.common.auth;

import com.sshtools.common.ssh.Context;

public interface AbstractAuthenticationProtocol<C extends Context> {

	public final static int SSH_MSG_USERAUTH_REQUEST = 50;
	public final static int SSH_MSG_USERAUTH_FAILURE = 51;
	public final static int SSH_MSG_USERAUTH_SUCCESS = 52;
	public final static int SSH_MSG_USERAUTH_BANNER = 53;
	
	void completedAuthentication();

	void failedAuthentication();

	void discardAuthentication();

	boolean canContinue();

	void markFailed();

	void failedAuthentication(boolean partial, boolean ignoreFailed);

}
