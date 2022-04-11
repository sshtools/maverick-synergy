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
package com.sshtools.server;

import java.io.IOException;
import java.net.InetSocketAddress;
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

//#ifdef LICENSE
//import com.sshtools.synergy.common.nio.LicenseManager;
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
	
	private void processProxyProtocol(String tmp) throws IOException {
		
		if(!getContext().getPolicy(LoadBalancerPolicy.class).isProxyProtocolEnabled()) {
			throw new IOException("Received PROXY protocol directive but the current policy does not support it");
		}
		
		if(getContext().getPolicy(LoadBalancerPolicy.class).isRestrictedAccess()) {
			String remoteAddress = ((InetSocketAddress)socketConnection.getRemoteAddress()).getAddress().getHostAddress();
			if(!getContext().getPolicy(LoadBalancerPolicy.class).isSupportedIPAddress(remoteAddress)) {
				throw new IOException(String.format("Received PROXY protocol string from unsupported IP address %s", remoteAddress));
			}
			if(Log.isDebugEnabled()) {
				Log.debug("PROXY protocol directive enabled by remote IP adresss {}", remoteAddress);
			}
		}
		
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
	protected void processNegotiationString(String value) throws IOException {
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
	
//	@Override
//	protected SocketAddress getConnectionAddress() {
//		return getRemoteAddress();
//	}

	@Override
	public void startService(com.sshtools.common.sshd.Service<SshServerContext> service) {
		
	}
}
