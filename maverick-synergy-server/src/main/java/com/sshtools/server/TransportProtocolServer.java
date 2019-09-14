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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.ConnectRequestFuture;
import com.sshtools.common.nio.LicenseException;
import com.sshtools.common.nio.SocketConnection;
import com.sshtools.common.permissions.IPPolicy;
import com.sshtools.common.ssh.ConnectionStateListener;
import com.sshtools.common.ssh.Service;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.TransportProtocol;
import com.sshtools.common.ssh.components.SshKeyExchange;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.sshd.AbstractServerTransport;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;

//#ifdef LICENSE
//import com.sshtools.common.nio.LicenseManager;
//#endif

public final class TransportProtocolServer extends TransportProtocol<SshServerContext> implements AbstractServerTransport<SshServerContext> {

	int disconnectReason;
	String disconnectText;
	boolean denyConnection = false;

	//#ifdef LICENSE
	//final static LicenseVerification license = new LicenseVerification();
	//#endif
	
	public TransportProtocolServer(SshServerContext sshContext, ConnectRequestFuture connectFuture) throws LicenseException {
		super(sshContext, connectFuture);
		//#ifdef LICENSE
		//checkLicensing();
		//#endif
	}

	//#ifdef LICENSE
	/*
	 private final void checkLicensing() throws LicenseException {
		
		if(!license.isLicensed()) {
			license.verifyLicense();
			
			if(license.isValid()) {
				if(Log.isInfoEnabled()) {
					Log.info("This Maverick NG API product is licensed to " + license.getLicensee());
				}
			}
		}
		
		switch (license.getStatus() & LicenseVerification.LICENSE_VERIFICATION_MASK) {
			case LicenseVerification.EXPIRED:
				throw new LicenseException("Your license has expired! visit http://www.sshtools.com to obtain an update version of the software.");
			case LicenseVerification.OK:
				break;
			case LicenseVerification.INVALID:
				throw new LicenseException("Your license is invalid!");
			case LicenseVerification.NOT_LICENSED:
				throw new LicenseException("NOT_LICENSED_TEXT");
			case LicenseVerification.EXPIRED_MAINTENANCE:
				throw new LicenseException(
						"Your support and maintenance has expired! visit http://www.sshtools.com to purchase a subscription");
			default:
				throw new LicenseException("An unexpected license status was received.");
		}
	}
	*/
	//#endif
	
	public SshServerContext getContext() {
		return sshContext;
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

			if (sshContext.getMaximumConnections() > -1) {

				if (numberOfConnections.intValue() >= sshContext.getMaximumConnections()) {
					denyConnection = true;
					disconnectText = getContext().getTooManyConnectionsText();
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
		keyExchange.init(this, remoteIdentification.toString().trim(), localIdentification.trim(), remotekex, localkex,
				pair.getPrivateKey(), pair.getPublicKey(), firstPacketFollows, useFirstPacket);

	}

	protected void keyExchangeInitialized() {
		if (denyConnection) {
			fireTooManyConnectionsDisconnectEvent(sshContext.getConnectionManager().getNumberOfConnections());
			disconnect(disconnectReason, disconnectText);
		}
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

					public void messageSent(Long sequenceNo) {
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

	void startService(Service activeService) {

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
		for (ConnectionStateListener<SshServerContext> stateListener : getContext().getStateListeners()) {
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
		// TODO Auto-generated method stub
		
	}
}
