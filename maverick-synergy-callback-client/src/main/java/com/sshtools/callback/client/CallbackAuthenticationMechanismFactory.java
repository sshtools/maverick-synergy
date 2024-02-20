package com.sshtools.callback.client;

/*-
 * #%L
 * Callback Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
