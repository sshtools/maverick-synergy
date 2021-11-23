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

package com.sshtools.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.IPPolicy;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.sshd.AbstractServerTransport;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.nio.LicenseException;
import com.sshtools.synergy.nio.SocketConnection;
import com.sshtools.synergy.ssh.ConnectionStateListener;
import com.sshtools.synergy.ssh.Service;
import com.sshtools.synergy.ssh.TransportProtocol;
import com.sshtools.synergy.ssh.components.SshKeyExchange;

public final class TransportProtocolServer extends TransportProtocol<SshServerContext> implements AbstractServerTransport<SshServerContext> {

	int disconnectReason;
	String disconnectText;
	boolean denyConnection = false;
	
	public TransportProtocolServer(SshServerContext sshContext, ConnectRequestFuture connectFuture) throws LicenseException {
		super(sshContext, connectFuture);
	}
	
	public SshServerContext getContext() {
		return sshContext;
	}
	
	private void processProxyProtocol(String tmp) {
		
		if(Log.isInfoEnabled()) {
			Log.info(String.format("Parsing PROXY protocol string [%s]", tmp));
		}
		
		String[] elements = tmp.split(" ");
		if(elements.length < 4) {
		
				if(Log.isInfoEnabled()) {
					Log.info("Not enough parameters in PROXY statement");
				}
				return;
		}
		
		if("TCP4".equals(elements[1]) || "TCP6".equals(elements[1])) {
			String sourceAddress = elements[2].trim();
			String targetAddress = elements[3].trim();
			int sourcePort = Integer.parseInt(elements[4].trim());
			int targetPort = Integer.parseInt(elements[5].trim());
			
			con.setRemoteAddress(InetSocketAddress.createUnresolved(sourceAddress, sourcePort));
			con.setLocalAddress(InetSocketAddress.createUnresolved(targetAddress, targetPort));
		}
		
	}

	@Override
	protected void processNegotiationString(String value) {
		if(value.startsWith("PROXY")) {
			processProxyProtocol(value);
		}
	}
	
	@Override
	protected boolean canConnect(SocketConnection connection) {

		boolean canConnect = sshContext.getPolicy(IPPolicy.class).checkConnection(connection.getRemoteAddress(),
				connection.getLocalAddress());

		if(Log.isDebugEnabled()) {
			Log.debug("IP policy has " + (canConnect ? "authorized" : "denied") + " access to "
					+ ((InetSocketAddress) connection.getRemoteAddress()).getAddress());
		}

		if (!canConnect) {
			return false;
		}
		synchronized (lock) {

			Integer numberOfConnections = sshContext.getConnectionManager().getNumberOfConnections();

			if (sshContext.getEngine().getContext().getMaximumConnections() > -1) {

				if (numberOfConnections.intValue() >= sshContext.getEngine().getContext().getMaximumConnections()) {
					denyConnection = true;
					disconnectText = sshContext.getEngine().getContext().getTooManyConnectionsText();
					disconnectReason = TransportProtocol.TOO_MANY_CONNECTIONS;

					if (!sshContext.isEnsureGracefulDisconnect()) {
						fireTooManyConnectionsDisconnectEvent(numberOfConnections);
						if(Log.isDebugEnabled())
							Log.debug("Denying connection.. too many users currently online");
						connection.closeConnection();
						return false;
					}

					/**
					 * Fix disconnect event when using getAllowedDeniedKEX() ==
					 * true
					 */
					sessionIdentifier = new byte[] {};
				}
			}
		}

		return true;
	}

	@Override
	protected void initializeKeyExchange(SshKeyExchange<SshServerContext> keyExchange, boolean firstPacketFollows,
			boolean useFirstPacket) throws IOException, SshException {

		SshKeyPair pair = getContext().getHostKey(publicKey);
		hostKey = pair.getPublicKey();
		keyExchange.init(this, remoteIdentification.toString().trim(), localIdentification.trim(), remotekex, localkex,
				pair.getPrivateKey(), pair.getPublicKey(), firstPacketFollows, useFirstPacket);

	}
	
	protected void onKeyExchangeInit() throws SshException {
		
		if(getContext().isForceServerPreferences()) {
			/**
			 * Reconfigure before sending kex init to ensure we only
			 * support the strongest algorithms of the client
			 */
			getContext().supportedKeyExchanges().removeAllBut(
					getContext().supportedKeyExchanges().selectStrongestComponent(getRemoteKeyExchanges()));
			getContext().supportedPublicKeys().removeAllBut(
					getContext().supportedPublicKeys().selectStrongestComponent(getRemotePublicKeys()));
			
			getContext().supportedCiphersCS().removeAllBut(
					getContext().supportedCiphersCS().selectStrongestComponent(getRemoteCiphersCS()));
			getContext().supportedCiphersCS().removeAllBut(
					getContext().supportedCiphersCS().selectStrongestComponent(getRemoteCiphersSC()));
			
			getContext().supportedMacsCS().removeAllBut(
					getContext().supportedMacsCS().selectStrongestComponent(getRemoteMacsCS()));
			getContext().supportedMacsSC().removeAllBut(
					getContext().supportedMacsSC().selectStrongestComponent(getRemoteMacsSC()));

		}
	}

	protected void keyExchangeInitialized() {
		if (denyConnection) {
			fireTooManyConnectionsDisconnectEvent(sshContext.getConnectionManager().getNumberOfConnections());
			disconnect(disconnectReason, disconnectText);
		}
	}

	@Override
	protected boolean canSendKeyExchangeInit() {
		return !getContext().isForceServerPreferences();
	}
	
	@Override
	protected void onNewKeysReceived() {
		generateNewKeysServerIn();
	}

	@Override
	protected boolean processTransportMessage(int msgId, byte[] msg) throws IOException {

		switch (msg[0]) {
		case SSH_MSG_SERVICE_REQUEST: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_MSG_SERVICE_REQUEST");
			startService(msg);
			return true;
		}
		default: {
			return false;
		}
		}
	}

	/**
	 * Request that the remote server starts a transport protocol service.
	 * 
	 * @param servicename
	 * @throws IOException
	 */
	void startService(byte[] msg) throws IOException {

		ByteArrayReader bar = new ByteArrayReader(msg);

		try {
			bar.skip(1);

			final String servicename = bar.readString();

			// Do we allow this service?
			if (servicename.equals("ssh-userauth")) {

				// Set the current service to the authentication protocol
				activeService = new AuthenticationProtocolServer(this);

				final byte[] serviceNameBytes = getBytes(servicename, CHARSET_ENCODING);
				// Inform the client that we have accepted the service
				postMessage(new SshMessage() {

					public boolean writeMessageIntoBuffer(ByteBuffer buf) {
						buf.put((byte) SSH_MSG_SERVICE_ACCEPT);
						buf.putInt(serviceNameBytes.length);
						buf.put(serviceNameBytes);
						return true;
					}

					public void messageSent(Long sequenceNo) throws SshException {
						if(Log.isDebugEnabled())
							Log.debug("Sent SSH_MSG_SERVICE_ACCEPT");
						activeService.start();
					}
				});

			} else {
				disconnect(SERVICE_NOT_AVAILABLE, servicename + " is not a valid service.");
			}
		} finally {
			bar.close();
		}

	}

	void startService(Service activeService) throws SshException {

		this.activeService.stop();
		this.activeService = activeService;
		activeService.start();

	}

	@Override
	protected void onNewKeysSent() {
		generateNewKeysServerOut();
	}

	@Override
	protected void disconnected() {
		for (ConnectionStateListener stateListener : getContext().getStateListeners()) {
			stateListener.disconnected(getContext().getConnectionManager().getConnectionById(getUUID()));
		}
	}

	@Override
	protected void onConnected() {
		this.con = getContext().getConnectionManager().registerTransport(this, getContext());
		getConnectFuture().connected(this, con);
	}

	@Override
	protected void onDisconnected() {
		getContext().getConnectionManager().unregisterTransport(this);
	}

	private void fireTooManyConnectionsDisconnectEvent(Integer numberOfConnections) {
		EventServiceImplementation.getInstance()
				.fireEvent((new Event(this, EventCodes.EVENT_REACHED_CONNECTION_LIMIT, false))
						.addAttribute(EventCodes.ATTRIBUTE_CONNECTION, con).addAttribute(
								EventCodes.ATTRIBUTE_NUMBER_OF_CONNECTIONS,
								String.valueOf(numberOfConnections.intValue())));

	}
	
	public String getName() {
		return "transport-server";
	}
	
	@Override
	protected SocketAddress getConnectionAddress() {
		return getRemoteAddress();
	}

	@Override
	public void startService(com.sshtools.common.sshd.Service<SshServerContext> service) {
		
	}
}
