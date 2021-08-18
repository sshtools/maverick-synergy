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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.sshd.AbstractServerTransport;

public class DefaultAuthenticationMechanismFactory<C extends Context>
		implements AuthenticationMechanismFactory<C> {

	protected Set<String> required = new HashSet<>();
	
	protected Set<String> supportedMechanisms = new HashSet<>();
	
	protected List<PasswordAuthenticationProvider> passwordProviders = new ArrayList<PasswordAuthenticationProvider>();
	protected List<PublicKeyAuthenticationProvider> publickeyProviders = new ArrayList<PublicKeyAuthenticationProvider>();
	protected List<KeyboardInteractiveAuthenticationProvider> keyboardInteractiveProviders = new ArrayList<KeyboardInteractiveAuthenticationProvider>();
	
	public DefaultAuthenticationMechanismFactory() {
	}
	
	public void addRequiredAuthentication(String req) {
		if(!supportedMechanisms.contains(req)) {
			throw new IllegalArgumentException(String.format("%s is not a supported authentication mechanism", req));
		}
		required.add(req);
	}
	
	public void removeRequiredAuthentication(String req) {
		if(!supportedMechanisms.contains(req)) {
			throw new IllegalArgumentException(String.format("%s is not a supported authentication mechanism", req));
		}
		required.remove(req);
	}
	
	public void addPasswordAuthenticationProvider(PasswordAuthenticationProvider provider) {
		passwordProviders.add(provider);
		supportedMechanisms.add("password");
		supportedMechanisms.add("keyboard-interactive");
	}
	
	public void removePasswordAuthenticationProvider(PasswordAuthenticationProvider provider) {
		passwordProviders.remove(provider);
		if(passwordProviders.size()==0) {
			supportedMechanisms.remove("password");
		}
	}

	public void addPublicKeyAuthenticationProvider(PublicKeyAuthenticationProvider provider) {
		publickeyProviders.add(provider);
		supportedMechanisms.add("publickey");
	}
	
	public void removePublicKeyAuthenticationProvider(PublicKeyAuthenticationProvider provider) {
		publickeyProviders.remove(provider);
		if(publickeyProviders.size()==0) {
			supportedMechanisms.remove("publickey");
		}
	}
	
	public void addKeyboardInteractiveProvider(KeyboardInteractiveAuthenticationProvider provider) {
		keyboardInteractiveProviders.add(provider);
		supportedMechanisms.add("keyboard-interactive");
	}
	
	public void removeKeyboardInteractiveProvider(KeyboardInteractiveAuthenticationProvider provider) {
		keyboardInteractiveProviders.remove(provider);
	}
	
	public void addProvider(Authenticator provider) {
		if(provider instanceof PasswordAuthenticationProvider) {
			addPasswordAuthenticationProvider((PasswordAuthenticationProvider)provider);
		} else if(provider instanceof PublicKeyAuthenticationProvider) {
			addPublicKeyAuthenticationProvider((PublicKeyAuthenticationProvider)provider);
		} else if(provider instanceof KeyboardInteractiveAuthenticationProvider) {
			addKeyboardInteractiveProvider((KeyboardInteractiveAuthenticationProvider)provider);
		} else
			throw new IllegalArgumentException(provider.getClass().getName() + " is not a supported AuthenticationProvider");
	}
	
	public AuthenticationMechanism createInstance(String name,
			AbstractServerTransport<C> transport,
			AbstractAuthenticationProtocol<C> authentication, SshConnection con)
			throws UnsupportedChannelException {
		
		if(name.equals(PasswordAuthentication.AUTHENTICATION_METHOD)) {
			return new PasswordAuthentication<C>(transport, authentication, con, getPasswordAuthenticationProviders(con));
		} else if(name.equals(PublicKeyAuthentication.AUTHENTICATION_METHOD)) {
			return new PublicKeyAuthentication<C>(transport, authentication, con, getPublicKeyAuthenticationProviders(con));
		} else if(name.equals(KeyboardInteractiveAuthentication.AUTHENTICATION_METHOD)) {
			return new KeyboardInteractiveAuthentication<C>(transport, authentication, con, getKeyboardInteractiveProviders(con));
		} 
		
		throw new UnsupportedChannelException();
	}
	
	public KeyboardInteractiveAuthenticationProvider[] getKeyboardInteractiveProviders(SshConnection con) {
		if(keyboardInteractiveProviders.size()==0) {
			return new KeyboardInteractiveAuthenticationProvider[] { new KeyboardInteractiveAuthenticationProvider() {
				public KeyboardInteractiveProvider createInstance(SshConnection con) {
					return new PasswordKeyboardInteractiveProvider(passwordProviders.toArray(new PasswordAuthenticationProvider[0]), con);
				}
			}};
		} else {
			return keyboardInteractiveProviders.toArray(new KeyboardInteractiveAuthenticationProvider[0]);
		}
	}
	
	public String[] getRequiredMechanisms(SshConnection con) {
		return required.toArray(new String[0]);
	}

	public String[] getSupportedMechanisms() {
		return supportedMechanisms.toArray(new String[0]);
	}

	public PublicKeyAuthenticationProvider[] getPublicKeyAuthenticationProviders(SshConnection con) {
		return publickeyProviders.toArray(new PublicKeyAuthenticationProvider[0]);
	}

	public PasswordAuthenticationProvider[] getPasswordAuthenticationProviders(SshConnection con) {
		return passwordProviders.toArray(new PasswordAuthenticationProvider[0]);
	}

	public Authenticator[] getProviders(String name, SshConnection con) {
		if(name.equals(PasswordAuthentication.AUTHENTICATION_METHOD)) {
			return getPasswordAuthenticationProviders(con);
		} else if(name.equals(PublicKeyAuthentication.AUTHENTICATION_METHOD)) {
			return getPublicKeyAuthenticationProviders(con);
		} else if(name.equals(KeyboardInteractiveAuthentication.AUTHENTICATION_METHOD)) {
			return getKeyboardInteractiveProviders(con);
		} 
		throw new IllegalArgumentException("Unknown provider type");
	}

	@Override
	public void addProviders(Collection<Authenticator> authenticators) {
		for(Authenticator authenticator : authenticators) {
			addProvider(authenticator);
		}
	}

	@Override
	public boolean isSupportedMechanism(String method) {
		return supportedMechanisms.contains(method);
	}

	

}
