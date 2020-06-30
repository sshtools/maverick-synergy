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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.common.auth.AbstractAuthenticationProtocol;
import com.sshtools.common.auth.AuthenticationMechanism;
import com.sshtools.common.auth.AuthenticationMechanismFactory;
import com.sshtools.common.auth.KeyboardInteractiveAuthentication;
import com.sshtools.common.auth.RequiredAuthenticationStrategy;
import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.policy.AuthenticationPolicy;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionProtocol;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.Service;
import com.sshtools.common.ssh.SshContext;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.TransportProtocol;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.Utils;

/**
 * This class implements the SSH Authentication Protocol. The only required
 * interaction with this class would be if you were creating an
 * {@link AuthenticationMechanism}. In this scenario you would simply call
 * either {@link completedAuthentication()} or {@link failedAuthentication()} to
 * inform the protocol that your authentication either completed or failed.
 * 
 * 
 */
public class AuthenticationProtocolServer extends ExecutorOperationSupport<SshContext> 
	implements Service, AbstractAuthenticationProtocol<SshServerContext> {

	TransportProtocolServer transport;
	boolean authInProgress = false;
	int failed = 0;
	AuthenticationMechanism currentAuthentication;
	String currentMethod;
	String username;
	String service;
	ArrayList<String> completedAuthentications = new ArrayList<String>();
	Map<String, Object> authenticationParameters = new ConcurrentHashMap<String, Object>(8, 0.9f, 1);
	Date started = new Date();
	String[] requiredAuthentications = null;
	boolean authenticated = false;
	boolean firstAttempt = false;
	
	/** The name of this service "ssh-userauth" */
	static final String SERVICE_NAME = "ssh-userauth";

	/**
	 * Construct the protocol using the given transport
	 * 
	 * @param transport
	 * @throws IOException
	 */
	public AuthenticationProtocolServer(TransportProtocolServer transport) {
		super("authentication-protocol");
		this.transport = transport;
	}

	public SshServerContext getContext() {
		return transport.getContext();
	}
	
	/**
	 * Called by the {@link TransportProtocol} when the service stops.
	 */
	public synchronized void stop() {
		if (transport != null) {
			if(Log.isDebugEnabled()) {
				Log.debug("Cleaning up authentication protocol references");
			}
			transport.getConnection().getAuthenticatedFuture().authenticated(authenticated);
		}
	}

	/**
	 * Called by the {@link TransportProtocol} when the service starts. Here we
	 * check for an authentication banner and send if configured.
	 */
	public void start() {
		/**
		 * Send a banner message if we have one configured
		 */
		if (Utils.isNotBlank(transport.getSshContext().getPolicy(AuthenticationPolicy.class).getBannerMessage())) {
			transport.postMessage(new SshMessage() {
				public boolean writeMessageIntoBuffer(ByteBuffer buf) {
					buf.put((byte) SSH_MSG_USERAUTH_BANNER);
					byte[] tmp = transport.getSshContext().getPolicy(AuthenticationPolicy.class).getBannerMessage()
							.getBytes();
					buf.putInt(tmp.length);
					buf.put(tmp);
					buf.putInt(0);
					return true;
				}

				public void messageSent(Long sequenceNo) {
					if(Log.isDebugEnabled()) {
						Log.debug("Sent SSH_MSG_USERAUTH_BANNER");
					}
				}
			});
		}

	}

	/**
	 * Process an SSH message.
	 * 
	 * @param msg
	 *            the message to process
	 * @return <tt>true</tt> if the message was processed, otherwise
	 *         <tt>false</tt>
	 * @throws IOException
	 */
	public boolean processMessage(byte[] msg) throws IOException {

		if (authInProgress) {
			return currentAuthentication.processMessage(msg);
		}

		switch (msg[0]) {
		case SSH_MSG_USERAUTH_REQUEST:

			/** We have an authentication request **/
			processRequest(msg);
			return true;
		default:
			return false;
		}

	}

	public Object getParameter(String name) {
		return authenticationParameters.get(name);
	}

	public void setParameter(String name, Object value) {
		authenticationParameters.put(name, value);
	}

	/**
	 * Process an authentication message.
	 * 
	 * @param msg
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	void processRequest(byte[] msg) throws IOException {
		ByteArrayReader bar = new ByteArrayReader(msg);

		try {
			bar.skip(1);
			username = bar.readString();
			service = bar.readString();

			boolean canConnect = true;
			Connection<SshServerContext> con = transport.getConnection();
			con.setUsername(username);			
			
			if(!firstAttempt) {
				
				EventServiceImplementation
				.getInstance()
				.fireEvent(
						new Event(
								this,
								EventCodes.EVENT_USERAUTH_STARTED,
								true)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										transport.getConnection())
								.addAttribute(
										EventCodes.ATTRIBUTE_ATTEMPTED_USERNAME,
										username)
								.addAttribute(
										EventCodes.ATTRIBUTE_AUTHENTICATION_METHOD,
										currentMethod)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));
				
				firstAttempt = true;
			}
			
			if(requiredAuthentications == null
					|| transport.getSshContext().getPolicy(AuthenticationPolicy.class).getRequiredAuthenticationStrategy()
						==RequiredAuthenticationStrategy.ONCE_PER_AUTHENTICATION_ATTEMPT) {
				requiredAuthentications = transport
						.getSshContext()
						.getPolicy(AuthenticationMechanismFactory.class)
						.getRequiredMechanisms(con);
			}

			currentMethod = bar.readString();

			if(Log.isDebugEnabled()) {
				Log.debug("Client is attempting " + currentMethod
						+ " authentication");
			}

			byte[] request = null;
			if (bar.available() > 0) {
				request = new byte[bar.available()];
				bar.read(request);
			}

			if (canConnect
					&& transport.getContext().getPolicy(AuthenticationMechanismFactory.class)
							.isSupportedMechanism(currentMethod)
					&& service.equals(ConnectionProtocol.SERVICE_NAME)) {
				try {
					
					
					// New way
					currentAuthentication = transport
							.getContext()
							.getPolicy(AuthenticationMechanismFactory.class)
							.createInstance(currentMethod, transport, this, con);

					authInProgress = true;
					currentAuthentication.startRequest(username, request);
					return;

				} catch (UnsupportedChannelException ex) {
					if (!currentMethod
							.equals(AuthenticationMechanismFactory.NONE)) {
						if(Log.isErrorEnabled())
							Log.error("Failed to initialize " + currentMethod
									+ " authentication mechanism", ex);
					}
				}
			}

			// If we reach this far we have a failure
			failedAuthentication();
		} finally {
			bar.close();
		}

	}

	/**
	 * Each successful completion of an authentication method should call this
	 * method. The state of the authentication is then determined and if
	 * completed the SSH_MSG_USERAUTH_SUCCESS message is sent, if a further
	 * authentication is required, SSH_MSG_USERAUTH_FAILURE is sent with the
	 * partial value set to <tt>true</tt>.
	 * 
	 * @throws IOException
	 */
	public synchronized void completedAuthentication() {

		if(transport==null || !transport.isConnected()) {
			if(Log.isDebugEnabled()) {
				Log.debug("Transport is no longer connected!");
			}
			return;
		}
		
		if (currentAuthentication instanceof KeyboardInteractiveAuthentication) {
			if (((KeyboardInteractiveAuthentication<?>) currentAuthentication).getSelectedProvider().getName().equals("password")) {
				completedAuthentications.add("password");
			}
		}

		completedAuthentications.add(currentAuthentication.getMethod());

		boolean completed = true;

		for (int i = 0; i < requiredAuthentications.length; i++) {
			completed &= completedAuthentications
					.contains(requiredAuthentications[i]);
		}

		if (completed) {

			authenticated = true;
			
			// Send our success message and when sent start the Connection
			// Protocol
			transport.postMessage(new SshMessage() {
				public boolean writeMessageIntoBuffer(ByteBuffer buf) {
					buf.put((byte) SSH_MSG_USERAUTH_SUCCESS);
					return true;
				}

				public void messageSent(Long sequenceNo) throws SshException {
					// Start the new service
					if(Log.isDebugEnabled()) {
						Log.debug("Sent SSH_MSG_USERAUTH_SUCCESS method="
								+ currentMethod
								+ " completed="
								+ createList(completedAuthentications
										.toArray(new String[0])) + " required="
								+ createList(requiredAuthentications));
					}

					// This is actually a success but further authentications
					// are required
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
													currentMethod)
											.addAttribute(
													EventCodes.ATTRIBUTE_OPERATION_STARTED,
													started)
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
													completedAuthentications));

					for(ServerConnectionStateListener listener : getContext().getStateListeners()) {
						listener.authenticationComplete(transport.getConnection());
					}
					// This should be the last thing we do as this will set
					// transport to null
					transport.startService(new ConnectionProtocolServer(transport, username));
				}
			});

			authInProgress = false;
		} else {
			failedAuthentication(true, true);
		}

	}

	/**
	 * Inform the protocol that the current authentication should be discarded.
	 * This will result in no failure message being sent to the client.
	 * 
	 */
	public void discardAuthentication() {
		authInProgress = false;
	}

	/**
	 * A failed authentication attempt should call this method.
	 * 
	 * @throws IOException
	 */
	public synchronized void failedAuthentication() {
		failedAuthentication(false, false);
	}

	public synchronized void failedAuthentication(final boolean partial, boolean ignoreFailed)  {

		if(transport==null || !transport.isConnected()) {
			if(Log.isDebugEnabled()) {
				Log.debug("Transport is no longer connected!");
			}
			return;
		}
		
		String[] supported;
		
		supported = transport.getContext().getPolicy(AuthenticationMechanismFactory.class).getSupportedMechanisms();

		if(Boolean.getBoolean("maverick.oldMethodsToContinue")) {
			
			failedAuthentication(partial, ignoreFailed, supported);
			
		} else {
			ArrayList<String> tmp = new ArrayList<String>();
			for (int i = 0; i < supported.length; i++) {
				if (!completedAuthentications.contains(supported[i])) {
					tmp.add(supported[i]);
				}
			}
	
			failedAuthentication(partial, ignoreFailed,
					(String[]) tmp.toArray(new String[0]));
		}
	}

	/**
	 * Called by the completeAuthentication method if a partial authentication
	 * success occurred.
	 * 
	 * @param partial
	 *            the authentication succeeded but another authentication is
	 *            required
	 * @param ignoreFailed
	 *            don't count this as a failed authentication
	 * @throws IOException
	 */
	private synchronized void failedAuthentication(final boolean partial,
			final boolean ignoreFailed, final String[] methodsToContinue) {

	
		final String[] methods;
		if (methodsToContinue == null || methodsToContinue.length == 0) {
			
			methods = transport.getContext().getPolicy(AuthenticationMechanismFactory.class).getSupportedMechanisms();
		} else {
			methods = methodsToContinue;
		}

		fireFailureEvent(partial, ignoreFailed, methodsToContinue);

		if (!currentMethod.equals("none") && !partial) {

			if (!ignoreFailed) {
				failed++;
			}

			if (failed >= transport.getSshContext().getPolicy(AuthenticationPolicy.class).getMaxAuthentications()) {
				transport.disconnect(TransportProtocol.BY_APPLICATION,
						"Too many bad authentication attempts!");
				return;
			}
		}

		authInProgress = false;
		transport.postMessage(new SshMessage() {
			public boolean writeMessageIntoBuffer(ByteBuffer buf) {
				buf.put((byte) SSH_MSG_USERAUTH_FAILURE);

				String list = createList(methods);

				buf.putInt(list.length());
				buf.put(list.getBytes());
				buf.put((byte) (partial ? 1 : 0));
				return true;
			}

			public void messageSent(Long sequenceNo) {

				if(Log.isDebugEnabled()) {
					Log.debug("Sent SSH_MSG_USERAUTH_FAILURE method="
							+ currentMethod + " availableMethods="
							+ createList(methodsToContinue) + " partial="
							+ partial + " required="
							+ createList(requiredAuthentications));
				}
			}
		});


		
	}

	private void fireFailureEvent(boolean partial, boolean ignoreFailed,
			String[] methodsToContinue) {
		if (!currentMethod.equals("none")) {
			if (!partial) {
				if (!ignoreFailed) {
					EventServiceImplementation
							.getInstance()
							.fireEvent(
									new Event(
											this,
											EventCodes.EVENT_USERAUTH_FAILURE,
											true)
											.addAttribute(
													EventCodes.ATTRIBUTE_CONNECTION,
													transport.getConnection())
											.addAttribute(
													EventCodes.ATTRIBUTE_ATTEMPTED_USERNAME,
													username)
											.addAttribute(
													EventCodes.ATTRIBUTE_AUTHENTICATION_METHODS,
													createList(methodsToContinue))
											.addAttribute(
													EventCodes.ATTRIBUTE_AUTHENTICATION_METHOD,
													currentMethod));
				}

			} else {
				EventServiceImplementation
						.getInstance()
						.fireEvent(
								new Event(this,
										EventCodes.EVENT_USERAUTH_SUCCESS,
										true)
										.addAttribute(
												EventCodes.ATTRIBUTE_CONNECTION,
												transport.getConnection())
										.addAttribute(
												EventCodes.ATTRIBUTE_ATTEMPTED_USERNAME,
												username)
										.addAttribute(
												EventCodes.ATTRIBUTE_AUTHENTICATION_METHODS,
												createList(methodsToContinue))
										.addAttribute(
												EventCodes.ATTRIBUTE_AUTHENTICATION_METHOD,
												currentMethod));
			}
		}
	}

	private String createList(String[] methods) {

		String list = "";
		for (int i = 0; i < methods.length; i++) {
			list += ((i > 0) ? "," : "") + methods[i];
		}
		return list;
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

	public String getName() {
		return "ssh-userauth";
	}

	public boolean canContinue() {
		return failed <= getContext().getPolicy(AuthenticationPolicy.class).getMaxAuthentications();
	}
	
	public void markFailed() {
		failed++;
	}
}
