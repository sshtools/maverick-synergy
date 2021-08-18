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
