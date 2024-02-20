package com.sshtools.common.auth;

import java.util.Collection;

import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.sshd.AbstractServerTransport;

public interface AuthenticationMechanismFactory<C extends Context> {
	
	public static final String NONE = "none";
	public static final String PASSWORD_AUTHENTICATION = "password";
	public static final String PUBLICKEY_AUTHENTICATION = "publickey";
	public static final String KEYBOARD_INTERACTIVE_AUTHENTICATION = "keyboard-interactive";
	
	AuthenticationMechanism createInstance(String name,
			AbstractServerTransport<C> transport, 
			AbstractAuthenticationProtocol<C> authentication, 
			SshConnection con) throws UnsupportedChannelException;

	String[] getRequiredMechanisms(SshConnection con);
	
	String[] getSupportedMechanisms();
	
	Authenticator[] getProviders(String name, SshConnection con);

	void addProvider(Authenticator provider);

	void addProviders(Collection<Authenticator> providers);

	boolean isSupportedMechanism(String method);

}
