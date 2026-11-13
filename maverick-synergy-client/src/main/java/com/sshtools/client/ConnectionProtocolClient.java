package com.sshtools.client;

/*-
 * #%L
 * Client API
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.forwarding.ForwardingHandle;
import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.forwarding.ForwardingRequest.ForwardingRole;
import com.sshtools.common.forwarding.ForwardingRequest.Protocol;
import com.sshtools.common.knownhosts.HostKeyUpdater;
import com.sshtools.common.knownhosts.HostKeyVerification;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.ChannelNG;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.ConnectionStateListener;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;
import com.sshtools.synergy.ssh.GlobalRequestHandler;
import com.sshtools.synergy.ssh.TransportProtocol;

/**
 * Implements the client side of the SSH connection protocol
 */
public class ConnectionProtocolClient extends ConnectionProtocol<SshClientContext> {

	private static final String HOSTKEYS_STANDARD_REQUEST = "hostkeys";
	private static final String HOSTKEYS_VENDOR_REQUEST = "hostkeys-00@openssh.com";
	private static final String HOSTKEYS_PROVE_STANDARD_REQUEST = "hostkeys-prove";
	private static final String HOSTKEYS_PROVE_STANDARD_SIGNATURE_LABEL = "hostkeys-prove-0";
	private static final String HOSTKEYS_PROVE_VENDOR_REQUEST = "hostkeys-prove-00@openssh.com";
	private static final long HOSTKEYS_PROVE_TIMEOUT_MS = 30000L;

	
	public ConnectionProtocolClient(TransportProtocol<SshClientContext> transport, String username) {
		super(transport, username);
	}

	@Override
	protected boolean isClient() {
		return true;
	}
	
	@Override
	protected void onStart() {

		SshClientContext context = getContext();
		con = context.getConnectionManager().registerConnection(this);
		addGlobalRequestHandler(new HostKeysGlobalRequestHandler());

		addTask(ExecutorOperationSupport.EVENTS, new ConnectionTaskWrapper(con, new Runnable() {
			public void run() {
				for (ConnectionStateListener stateListener : getContext().getStateListeners()) {
					stateListener.ready(con);
				}
			}
		}));

	}

	protected void onStop() {
		
	}
	
	/**
	 * Start local port forwarding. Listening on a local interface and forwarding the data to a host on the remote network.
	 * 
	 * @param addressToBind
	 * @param portToBind
	 * @param destinationHost
	 * @param destinationPort
	 * @return
	 * @throws UnauthorizedException
	 * @throws SshException
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public int startLocalForwarding(String addressToBind, int portToBind, String destinationHost, int destinationPort)
			throws UnauthorizedException, SshException {

		return bindLocal(ForwardingRequest.ofTcp(addressToBind, portToBind, destinationHost, destinationPort)).boundPort().orElse(portToBind);
	}
	
	/**
	 * Start local port forwarding. Listening on a local interface and forwarding the data to a host on the remote network.
	 * 
	 * @param request local port forwarding
	 * @return handle
	 * @throws UnauthorizedException
	 * @throws SshException
	 */
	public ForwardingHandle bindLocal(ForwardingRequest request)
			throws UnauthorizedException, SshException {

		if(Log.isInfoEnabled()) {
			Log.info("Requesting local forwarding on " + request);
		}

		if (!getContext().getForwardingPolicy().validate(con, ForwardingRole.BIND, ForwardingRequest.ForwardingType.LOCAL, request)) {
			if(Log.isInfoEnabled()) {
				Log.info("User not permitted to forward on " + request);
			}
			throw new UnauthorizedException();
		}

		ForwardingHandle hndl = getContext().getForwardingManager().bindLocal(request, con);
		
		if(Log.isInfoEnabled()) {
			Log.info("Local forwarding is now active on local interface " + hndl);
		}
		
		return hndl;
	}
	
	/**
	 * Start remote port forwarding. Listening on a remote interface and forwarding to this host.
	 * 
	 * @param request request
	 * @return handle
	 * @throws UnauthorizedException
	 * @throws SshException
	 */
	public ForwardingHandle bindRemote(ForwardingRequest request)
			throws UnauthorizedException, SshException {

		if(Log.isInfoEnabled()) {
			Log.info("Requesting remote forwarding on " + request);
		}

		if (!getContext().getForwardingPolicy().validate(con, ForwardingRole.BIND, ForwardingRequest.ForwardingType.REMOTE, request)) {
			if(Log.isInfoEnabled()) {
				Log.info("User not permitted to forward on " + request);
			}
			throw new UnauthorizedException();
		}

		ForwardingHandle hndl = getContext().getForwardingManager().bindRemote(request, getConnection().getConnectionProtocol());
		if(Log.isInfoEnabled()) {
			Log.info("Remote forwarding is now active on local interface " + hndl);
		}
		
		return hndl;
	}

	/**
	 * Stop all bound local port forwards.
	 * 
	 * @throws IOException on any error.
	 */
	public void closeAllLocal() throws IOException {
		getContext().getForwardingManager().closeAllLocal(getConnection());
	}

	/**
	 * Stop all bound remote port forwards.
	 * 
	 * @throws IOException on any error.
	 */
	public void closeAllRemote() throws IOException {
		getContext().getForwardingManager().closeAllRemote(getConnection().getConnectionProtocol());
	}

	@Deprecated(forRemoval = true, since = "3.2.0")
	public void stopLocalForwarding() {
		getContext().getForwardingManager().stopForwarding(getConnection());
	}

	@Deprecated(forRemoval = true, since = "3.2.0")
	public void stopLocalForwarding(String addressToBind, int portToBind) {
		stopLocalForwarding(addressToBind + ":" + portToBind);
	}

	@Deprecated(forRemoval = true, since = "3.2.0")
	public void stopLocalForwarding(String key) {
		getContext().getForwardingManager().stopForwarding(key, getConnection());
	}

	@Deprecated(forRemoval = true, since = "3.2.0")
	public void stopRemoteForwarding(String addressToBind, int portToBind) throws SshException {
		getContext().getForwardingManager().stopRemoteForwarding(addressToBind, portToBind, this);
	}

	@Deprecated(forRemoval = true, since = "3.2.0")
	public void stopRemoteForwarding() throws SshException {
		getContext().getForwardingManager().stopRemoteForwarding(this);
	}
	
	/**
	 * Start remote port forwarding. Requests that the server starts a listening socket on the remote network and delivers
	 * data to a host on the local network.
	 * @param addressToBind
	 * @param portToBind
	 * @param destinationHost
	 * @param destinationPort
	 * @return actual destination port
	 * @throws SshException
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public int startRemoteForwarding(String addressToBind,
			int portToBind, String destinationHost, int destinationPort) throws SshException {
		try {
			return bindRemote(ForwardingRequest.ofTcp(addressToBind, portToBind, destinationHost, destinationPort)).boundPort().orElse(portToBind);
		} catch (UnauthorizedException e) {
			throw new SshException(e);
		}
	}

	@Override
	public SshClientContext getContext() {
		return getTransport().getContext();
	}

	/**
	 * Process remote forwarding cancel request. This method does nothing since the client does not support opening 
	 * of remote forwarding channels.
	 */
	@Override
	protected boolean processForwardCancel(Protocol protocol, ByteArrayReader bar, ByteArrayWriter msg) throws IOException {
		return false;
	}

	/**
	 * Process a request for remote forwarding. This method does nothing since the client does not support opening 
	 * of remote forwarding channels.
	 */
	@Override
	protected boolean processForward(Protocol protocol, ByteArrayReader bar, ByteArrayWriter response) throws IOException {
		return false;
	}

	/**
	 * The name of the ssh service i.e. ssh-connection
	 */
	public String getName() {
		return "ssh-connection";
	}

	/**
	 * Create an SSH channel. This method delegates creation to the ChannelFactory installed on the current
	 * SshContext.
	 * @throws ChannelOpenException 
	 */
	@Override
	protected ChannelNG<SshClientContext> createChannel(String channeltype, Connection<SshClientContext> con)
			throws UnsupportedChannelException, PermissionDeniedException, ChannelOpenException {
		return getContext().getChannelFactory().createChannel(channeltype, con);
	}

	private void performHostKeysProve(List<SshPublicKey> keys, HostKeyUpdater updater, boolean standardNames)
			throws IOException, SshException {

		String proveRequestName = standardNames ? HOSTKEYS_PROVE_STANDARD_REQUEST : HOSTKEYS_PROVE_VENDOR_REQUEST;
		String signatureLabel = standardNames ? HOSTKEYS_PROVE_STANDARD_SIGNATURE_LABEL : HOSTKEYS_PROVE_VENDOR_REQUEST;

		List<SshPublicKey> verifyKeys = new ArrayList<SshPublicKey>();
		String knownHostName = getKnownHostName();

		for (SshPublicKey key : keys) {
			if (updater.isKnownHost(knownHostName, key)) {
				if (Log.isDebugEnabled()) {
					Log.debug("Already trusted host key {}:{}", key.getAlgorithm(), key.getFingerprint());
				}
				continue;
			}
			verifyKeys.add(key);
		}

		if (verifyKeys.isEmpty()) {
			return;
		}

		try (ByteArrayWriter msg = new ByteArrayWriter()) {
			for (SshPublicKey key : verifyKeys) {
				msg.writeBinaryString(key.getEncoded());
			}

			GlobalRequest request = new GlobalRequest(proveRequestName, con, msg.toByteArray());
			sendGlobalRequestAndWait(request, HOSTKEYS_PROVE_TIMEOUT_MS);

			if (!request.isDoneAndSuccess()) {
				if (Log.isDebugEnabled()) {
					Log.debug("Server rejected {} request. Not updating trust for proposed host keys.", proveRequestName);
				}
				return;
			}

			byte[] responseData = request.getData();
			if (responseData == null) {
				Log.warn("Server returned an empty {} response. Not updating trust for proposed host keys.", proveRequestName);
				return;
			}

			try (ByteArrayReader reader = new ByteArrayReader(responseData)) {
				for (SshPublicKey key : verifyKeys) {
					if (reader.available() <= 0) {
						Log.warn("Server returned fewer host key proof signatures than expected for {}.", proveRequestName);
						break;
					}

					byte[] signature = reader.readBinaryString();
					if (!verifyHostKeyProofSignature(signature, key, signatureLabel)) {
						Log.warn(
								"Server provided an invalid signature in response to {} for key {}:{}",
								proveRequestName,
								key.getAlgorithm(),
								key.getFingerprint());
					} else {
						if (Log.isDebugEnabled()) {
							Log.debug("Persisting verified host key {}:{}", key.getAlgorithm(), key.getFingerprint());
						}
						updater.updateHostKey(knownHostName, key);
					}
				}
			}
		}
	}

	private boolean verifyHostKeyProofSignature(byte[] signature, SshPublicKey key, String signatureLabel)
			throws IOException, SshException {

		try (ByteArrayWriter msg = new ByteArrayWriter()) {
			msg.writeString(signatureLabel);
			msg.writeBinaryString(getTransport().getSessionKey());
			msg.writeBinaryString(key.getEncoded());

			return key.verifySignature(signature, msg.toByteArray());
		}
	}

	private String getKnownHostName() {
		String host = getTransport().getConnectFuture().getHost();
		if (!Boolean.getBoolean("maverick.knownHosts.disablePortValidate")
				&& getTransport().getConnectFuture().getPort() != 22) {
			host = "[" + host + "]:" + getTransport().getConnectFuture().getPort();
		}
		return host;
	}

	class HostKeysGlobalRequestHandler implements GlobalRequestHandler<SshClientContext> {

		@Override
		public String[] supportedRequests() {
			return new String[] { HOSTKEYS_STANDARD_REQUEST, HOSTKEYS_VENDOR_REQUEST };
		}

		@Override
		public boolean processGlobalRequest(GlobalRequest request, ConnectionProtocol<SshClientContext> connection,
				boolean wantsReply, ByteArrayWriter response)
				throws GlobalRequestHandler.GlobalRequestHandlerException, IOException {

			HostKeyVerification hostKeyVerification = getContext().getHostKeyVerification();
			if (!(hostKeyVerification instanceof HostKeyUpdater)) {
				if (Log.isDebugEnabled()) {
					Log.debug("Remote server sent updated host keys but HostKeyUpdater is not configured");
				}
				return false;
			}

			final boolean standardNames = HOSTKEYS_STANDARD_REQUEST.equals(request.getName());
			final List<SshPublicKey> keys = new ArrayList<SshPublicKey>();

			try (ByteArrayReader reader = new ByteArrayReader(request.getData())) {
				while (reader.available() > 0) {
					try {
						SshPublicKey key = SshPublicKeyFileFactory.decodeSSH2PublicKey(reader.readBinaryString());
						keys.add(key);
						if (Log.isDebugEnabled()) {
							Log.debug("Received host key for verification {}:{}", key.getAlgorithm(), key.getFingerprint());
						}
					} catch (Throwable t) {
						Log.warn(
								"Failed to parse server host key provided by {}. This may indicate an unsupported key type.",
								request.getName(),
								t);
					}
				}
			}

			if (!keys.isEmpty()) {
				new Thread(new ProveHostKeysTask(keys, (HostKeyUpdater) hostKeyVerification, standardNames),
						"synergy-hostkeys-prove").start();
			}

			return true;
		}
	}

	class ProveHostKeysTask implements Runnable {

		private final List<SshPublicKey> keys;
		private final HostKeyUpdater updater;
		private final boolean standardNames;

		ProveHostKeysTask(List<SshPublicKey> keys, HostKeyUpdater updater, boolean standardNames) {
			this.keys = keys;
			this.updater = updater;
			this.standardNames = standardNames;
		}

		@Override
		public void run() {
			try {
				performHostKeysProve(keys, updater, standardNames);
			} catch (IOException | SshException e) {
				Log.error("Host key update failed", e);
			}
		}
	}

}
