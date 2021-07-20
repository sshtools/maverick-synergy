/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

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
