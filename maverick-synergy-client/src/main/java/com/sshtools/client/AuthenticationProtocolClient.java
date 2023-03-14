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

package com.sshtools.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventListener;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;
import com.sshtools.synergy.ssh.Service;
import com.sshtools.synergy.ssh.TransportProtocol;
import com.sshtools.synergy.ssh.TransportProtocolListener;

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
	List<String> completedAuths = new ArrayList<>();
	boolean authenticated = false;
	int attempts;
	NoneAuthenticator noneAuthenticator = new NoneAuthenticator();
	Date methodStarted = new Date();
	Date authenticationStarted = new Date();
	
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

		transport.addEventListener(new TransportProtocolListener() {
			@Override
			public void onDisconnect(TransportProtocol<?> transport) {
				if(currentAuthenticator != null) {
					synchronized(currentAuthenticator) {
						currentAuthenticator.notifyAll();
					}
				}
			}
		});
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
				completedAuths.add(currentAuthenticator.getName());
				if(Log.isDebugEnabled()) {
					Log.debug("Received SSH_MSG_USERAUTH_SUCCESS");
				}
	
				EventServiceImplementation
				.getInstance()
				.fireEvent(
						new Event(
								this,
								EventCodes.EVENT_USERAUTH_SUCCESS,
								true)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										transport.getConnection())
								.addAttribute(
										EventCodes.ATTRIBUTE_ATTEMPTED_USERNAME,
										username)
								.addAttribute(
										EventCodes.ATTRIBUTE_AUTHENTICATION_METHOD,
										currentAuthenticator.getName())
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										methodStarted)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));

				EventServiceImplementation
						.getInstance()
						.fireEvent(
								new Event(
										this,
										EventCodes.EVENT_AUTHENTICATION_COMPLETE,
										true)
										.addAttribute(
												EventCodes.ATTRIBUTE_CONNECTION,
												transport.getConnection())
										.addAttribute(
												EventCodes.ATTRIBUTE_AUTHENTICATION_METHODS,
												completedAuths)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_STARTED,
												authenticationStarted)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_FINISHED,
												new Date()));

				
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
					Log.debug("Received SSH_MSG_USERAUTH_FAILURE auths=" + auths);
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
					completedAuths.add(currentAuthenticator.getName());
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
			
			methodStarted = new Date();
			currentAuthenticator = authenticators.removeFirst();
			
			if(Log.isDebugEnabled()) {
				Log.debug("Starting {} authentication", currentAuthenticator.getName());
			}
			attempts++;
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
						(supportedAuths.contains("password") || context.getPreferKeyboardInteractiveOverPassword())) {
					
					if(Log.isDebugEnabled()) {
						Log.debug("We prefer keyboard-interactive over password so injecting keyboard-interactive authenticator");
					}

					ClientAuthenticator kbi = new KeyboardInteractiveAuthenticator(
						new PasswordOverKeyboardInteractiveCallback(
								((PasswordAuthenticator) authenticator))) {
							@Override
							public synchronized void done(boolean success) {
								if(success || (!success && !supportedAuths.contains("password"))) {
									((PasswordAuthenticator)authenticator).done(success);
								} else {
									if(supportedAuths.contains("password")) {
										authenticators.addLast(authenticator);
									}
								}
								super.done(success);
							}
					};
					
					authenticators.addLast(kbi); 
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

	@Override
	public String getIdleLog() {
		return String.format("%d authentication attempts made", attempts);
	}

}
