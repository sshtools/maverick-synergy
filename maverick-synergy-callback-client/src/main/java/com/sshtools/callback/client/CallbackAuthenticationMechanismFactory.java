package com.sshtools.callback.client;

import com.sshtools.common.auth.AbstractAuthenticationProtocol;
import com.sshtools.common.auth.AuthenticationMechanism;
import com.sshtools.common.auth.DefaultAuthenticationMechanismFactory;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.sshd.AbstractServerTransport;

public class CallbackAuthenticationMechanismFactory<C extends Context> extends DefaultAuthenticationMechanismFactory<C> {

	
	MutualCallbackAuthenticationProvider provider;
	
	public CallbackAuthenticationMechanismFactory(MutualCallbackAuthenticationProvider provider) {
		this.provider = provider;
		supportedMechanisms.add(MutualCallbackAuthenticationProvider.MUTUAL_KEY_AUTHENTICATION);
	}
	
	public AuthenticationMechanism createInstance(String name,
			AbstractServerTransport<C> transport,
			AbstractAuthenticationProtocol<C> authentication, SshConnection con)
			throws UnsupportedChannelException {
		
		if(name.equals(MutualCallbackAuthenticationProvider.MUTUAL_KEY_AUTHENTICATION)) {
			return new MutualCallbackAuthentication<C>(transport, authentication, con, provider);
		}
		
		return super.createInstance(name, transport, authentication, con);
		
	}

}
