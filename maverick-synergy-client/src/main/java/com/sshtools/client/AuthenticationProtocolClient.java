/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventListener;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;
import com.sshtools.synergy.ssh.Service;
import com.sshtools.synergy.ssh.TransportProtocol;

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
	LinkedList<ClientAuthenticator> authenticators = new LinkedList<>();
	ClientAuthenticator currentAuthenticator;
	Set<String> supportedAuths = null;
	boolean authenticated = false;
	NoneAuthenticator noneAuthenticator = new NoneAuthenticator();
	
	public AuthenticationProtocolClient(TransportProtocolClient transport,
			SshClientContext context, String username) {
		this.transport = transport;
		this.context = context;
		this.username = username;
		transport.getConnection().setUsername(username);
		
		transport.addTask(ExecutorOperationSupport.EVENTS, new ConnectionTaskWrapper(transport.getConnection(), new Runnable() {
			public void run() {
				for (ClientStateListener stateListener : context.getStateListeners()) {
					stateListener.authenticationStarted(AuthenticationProtocolClient.this, transport.getConnection());
				}
			}
		}));

		transport.getConnection().addEventListener(new EventListener() {
			@Override
			public void processEvent(Event evt) {
				switch(evt.getId()) {
				case EventCodes.EVENT_DISCONNECTED:
					
					if(Objects.nonNull(currentAuthenticator)) {
						if(!currentAuthenticator.isDone()) {
							currentAuthenticator.failure();
						}
						currentAuthenticator = null;
					}
					authenticators.clear();
					
					if(!transport.getConnection().getAuthenticatedFuture().isDone()) {
						transport.getConnection().getAuthenticatedFuture().done(false);
					}
					break;
				default:
					break;
				}
			}
		});
	}

	@Override
	public boolean processMessage(byte[] msg) throws IOException, SshException {

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
	
				ConnectionProtocol<SshClientContext> con = new ConnectionProtocolClient(
						transport, username);
				stop();
				transport.setActiveService(con);
				con.start();
				
				currentAuthenticator.success();
				
				return true;
			case SSH_MSG_USERAUTH_FAILURE:
	
				String auths = bar.readString();
				final boolean partial = bar.readBoolean();
				
				if(Log.isDebugEnabled()) {
					Log.debug("SSH_MSG_USERAUTH_FAILURE received auths=" + auths);
				}
	
				StringTokenizer t = new StringTokenizer(auths, ",");
				supportedAuths = new HashSet<String>();
				
				while(t.hasMoreTokens()) {
					supportedAuths.add(t.nextToken());
				}

				if(currentAuthenticator.getName().equals("none")) {
					transport.getConnectFuture().connected(transport, transport.getConnection());
				} 
				
				if(partial) {
					currentAuthenticator.success(true, auths.split(","));
				} else {
					currentAuthenticator.failure();
				}
				
				if(!doNextAuthentication()) {
					transport.addTask(ExecutorOperationSupport.EVENTS, new ConnectionTaskWrapper(transport.getConnection(), new Runnable() {
						public void run() {
							for (ClientStateListener stateListener : context.getStateListeners()) {
								stateListener.authenticate(AuthenticationProtocolClient.this, transport.getConnection(), supportedAuths, partial);
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

	public void start() throws SshException {

		if(Log.isDebugEnabled()) {
			Log.debug("Starting Authentication Protocol");
		}

		try {
			authenticators.add(noneAuthenticator);
			if(!context.getAuthenticators().isEmpty()) {
				authenticators.addAll(context.getAuthenticators());
			}
			doNextAuthentication();
		} catch (IOException e) {
			Log.error("Faild to send none authentication request", e);
			transport.disconnected();
		}

	}
	
	public synchronized boolean doNextAuthentication() throws IOException, SshException {

		if(!authenticators.isEmpty()) {
			
			currentAuthenticator = authenticators.removeFirst();
			
			if(Log.isDebugEnabled()) {
				Log.debug("Starting {} authentication", currentAuthenticator.getName());
			}
			currentAuthenticator.authenticate(transport, username);
			return true;
		} 
		
		return false;
	}

	public void stop() {
		if(Log.isDebugEnabled()) {
			Log.debug("Stopping Authentication Protocol");
		}
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

	public synchronized void addAuthentication(Collection<ClientAuthenticator> auths) throws IOException, SshException {	
		for(ClientAuthenticator auth : auths) {
			addAuthentication(auth);
		}
	}
	
	public void addAuthentication(ClientAuthenticator authenticator) throws IOException, SshException {
		
		checkReady();
		
		synchronized(this) {
			if(Log.isDebugEnabled()) {
				Log.debug("Adding {} authentication", authenticator.getName());
			}
			
			boolean start = authenticators.isEmpty();
			
			if(authenticator instanceof PasswordAuthenticator) {
				if(supportedAuths.contains("keyboard-interactive") &&
						context.getPreferKeyboardInteractiveOverPassword()) {
					
					if(Log.isDebugEnabled()) {
						Log.debug("We prefer keyboard-interactive over password so injecting keyboard-interactive authenticator");
					}

					authenticators.addLast(new KeyboardInteractiveAuthenticator(
							new PasswordOverKeyboardInteractiveCallback(
									((PasswordAuthenticator) authenticator).getPassword())) {

						@Override
						public synchronized void done(boolean success) {
							if(success || (!success && !supportedAuths.contains("password"))) {
								((PasswordAuthenticator)authenticator).done(success);
							}
							super.done(success);
						}
							
						
					}); 
					
					if(supportedAuths.contains("password")) {
						authenticators.addLast(authenticator);
					}
				}
				else {
					authenticators.addLast(authenticator);
				}
				
			} else {
				authenticators.addLast(authenticator);
			}
	
			if(start) {
				doNextAuthentication();
			}
		}
		
	}
	
	private void checkReady() throws IOException {
		if(transport.getDisconnectFuture().isDone()) {
			throw new IOException("SSH client has been disconnected!");
		}
		if(!noneAuthenticator.isDone()) {
			if(Log.isDebugEnabled()) {
				Log.debug("Authentication protocol is NOT ready");
			}
			noneAuthenticator.waitFor(30000);
			if(!noneAuthenticator.isDone()) {
				throw new IOException("Timeout waiting for authentication protocol to start");
			}
			if(Log.isDebugEnabled()) {
				Log.debug("Authentication protocol is ready");
			}
		}
	}

	public Set<String> getSupportedAuthentications() {
		return supportedAuths;
	}

}
