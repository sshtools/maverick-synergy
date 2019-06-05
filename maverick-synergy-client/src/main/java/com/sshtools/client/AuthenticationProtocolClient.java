/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ConnectionProtocol;
import com.sshtools.common.ssh.ConnectionTaskWrapper;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.Service;
import com.sshtools.common.ssh.TransportProtocol;
import com.sshtools.common.util.ByteArrayReader;

/**
 * Implements the client side of the SSH authentication protocol.
 */
public class AuthenticationProtocolClient implements Service {

	public final static int SSH_MSG_USERAUTH_REQUEST = 50;
	public final static int SSH_MSG_USERAUTH_FAILURE = 51;
	public final static int SSH_MSG_USERAUTH_SUCCESS = 52;
	public final static int SSH_MSG_USERAUTH_BANNER = 53;

	int authIndex = 0;
	TransportProtocolClient transport;
	SshClientContext context;
	String username;
	List<ClientAuthenticator> authenticators;
	ClientAuthenticator currentAuthenticator;
	Set<String> supportedAuths = null;
	boolean authenticated = false;
	
	public AuthenticationProtocolClient(TransportProtocolClient transport,
			SshClientContext context, String username) {
		this.transport = transport;
		this.context = context;
		this.username = username;
	}

	@Override
	public boolean processMessage(byte[] msg) throws IOException {

		ByteArrayReader bar = new ByteArrayReader(msg);
		
		try {
	
			/**
			 * Try the authenticator first. It may want to handle 
			 */
			if (currentAuthenticator != null) {
				if(currentAuthenticator.processMessage(bar)) {
					return true;
				}
				bar.reset();
			} 
			
			bar.skip(1);
			
			switch (msg[0]) {
			case SSH_MSG_USERAUTH_SUCCESS:
	
				authenticated = true;
				if(Log.isDebugEnabled()) {
					Log.debug("SSH_MSG_USERAUTH_SUCCESS received");
				}
	
				currentAuthenticator.success();
				
				ConnectionProtocol<SshClientContext> con = new ConnectionProtocolClient(
						transport, username);
				stop();
				transport.setActiveService(con);
				con.start();
				
				return true;
			case SSH_MSG_USERAUTH_FAILURE:
	
				String auths = bar.readString();
				final boolean partial = bar.readBoolean();
				
				if(currentAuthenticator==null) {
					transport.getConnectFuture().connected(transport, transport.getConnection());
				} else {
					if(partial) {
						currentAuthenticator.success();
					} else {
						currentAuthenticator.failure();
					}
				}
				
				if(Log.isDebugEnabled()) {
					Log.debug("SSH_MSG_USERAUTH_FAILURE received auths=" + auths);
				}
	
				boolean checkKBI = supportedAuths == null;
				
				StringTokenizer t = new StringTokenizer(auths, ",");
				supportedAuths = new HashSet<String>();
				
				while(t.hasMoreTokens()) {
					supportedAuths.add(t.nextToken());
				}
				
				if(checkKBI) {
					checkForKeyboardInteractiveAuthentication();
				}
				
				if(canAuthenticate()) {
					doNextAuthentication();
				}  else {
					transport.addTask(ExecutorOperationSupport.EVENTS, new ConnectionTaskWrapper(transport.getConnection(), new Runnable() {
						public void run() {
							List<ClientAuthenticator> auths = new ArrayList<ClientAuthenticator>();
							for (ClientStateListener stateListener : context.getStateListeners()) {
								stateListener.authenticate(transport.getConnection(), supportedAuths, partial, auths);
								authenticators.addAll(auths);
							}
							if(canAuthenticate()) {
								doNextAuthentication();
							} 
						}
					}));
				}

				return true;
			case SSH_MSG_USERAUTH_BANNER:
	
				String banner = bar.readString();
				if (context.getBannerDisplay() != null) {
					context.getBannerDisplay().displayBanner(banner);
				}
	
				if(Log.isDebugEnabled()) {
					Log.debug("SSH_MSG_USERAUTH_BANNER received");
					Log.debug(bar.readString());
				}
	
				return true;
			}
	
			return false;

		} finally {
			bar.close();
		}
	}

	private void checkForKeyboardInteractiveAuthentication() {
		
		int passwordIdx = -1;
		boolean hasKBI = false;

		int idx = 0;
		for (ClientAuthenticator auth : authenticators) {
			if (auth instanceof PasswordAuthenticator) {
				passwordIdx = idx;
			}
			if (auth instanceof KeyboardInteractiveAuthenticator) {
				hasKBI = true;
			}
			idx++;
		}

		/**
		 * Inject KBI if user has not provided it so we can perform
		 * password over KBI in preference to password.
		 */
		if (!hasKBI && passwordIdx > -1 && supportedAuths.contains("keyboard-interactive")) {
			authenticators.add(passwordIdx, new KeyboardInteractiveAuthenticator(
					new PasswordOverKeyboardInteractiveCallback(
							(PasswordAuthenticator) authenticators
									.get(passwordIdx))));
		}
		
	}

	public void start() {

		if(Log.isDebugEnabled()) {
			Log.debug("Starting Authentication Protocol");
		}

		authenticators = new ArrayList<ClientAuthenticator>(
				context.getAuthenticators());

		doNoneAuthentication();

	}

	private void doNoneAuthentication() {
		transport.postMessage(new AuthenticationMessage(username,
				"ssh-connection", "none"));
	}

	private boolean canAuthenticate() {
		return  authIndex < authenticators.size();
	}
	
	public void doNextAuthentication() {

		if (canAuthenticate()) {
			currentAuthenticator = authenticators.get(authIndex++);
			currentAuthenticator.authenticate(transport, username);
		}
	}

	public void stop() {
		if(Log.isDebugEnabled()) {
			Log.debug("Stopping Authentication Protocol");
		}
		transport.getConnection().getAuthenticatedFuture().authenticated(authenticated);
	}

	@Override
	public String getName() {
		return "ssh-userauth";
	}

	@Override
	public int getIdleTimeoutSeconds() {
		return transport.getContext().getIdleAuthenticationTimeoutSeconds();
	}
	
	@Override
	public boolean idle() {
		transport.disconnect(TransportProtocol.BY_APPLICATION, "Idle unauthenticated connection");
		return true;
	}

	public void doAuthentication(ClientAuthenticator authenticator) {
		currentAuthenticator = authenticator;
		currentAuthenticator.authenticate(transport, username);
	}

}
