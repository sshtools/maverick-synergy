package com.sshtools.common.auth;

import java.io.IOException;
import java.util.Collection;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh2.KBIPrompt;

public interface KeyboardInteractiveProvider {

	KBIPrompt[] init(SshConnection con);
	boolean setResponse(String[] answers, Collection<KBIPrompt> additionalPrompts) throws IOException;
	String getName();
	String getInstruction();
	boolean hasAuthenticated();
	
}
