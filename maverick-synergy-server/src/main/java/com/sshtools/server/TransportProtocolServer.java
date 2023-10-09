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
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.nio.LicenseException;
import com.sshtools.synergy.nio.SocketConnection;
import com.sshtools.synergy.ssh.ConnectionStateListener;
import com.sshtools.synergy.ssh.Service;
import com.sshtools.synergy.ssh.SshContext;
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
	
	private void processProxyProtocol(String tmp) throws IOException {
		
		LoadBalancerPolicy policy = getContext().getPolicy(LoadBalancerPolicy.class);
		
		if(!policy.isProxyProtocolEnabled()) {
			throw new IOException("Received PROXY protocol directive but the current policy does not support it");
		}
		
		if(policy.isRestrictedAccess()) {
			String remoteAddress = ((InetSocketAddress)socketConnection.getRemoteAddress()).getAddress().getHostAddress();
			if(!policy.isSupportedIPAddress(remoteAddress)) {
				throw new IOException(String.format("Received PROXY protocol string from unsupported IP address %s", remoteAddress));
			}
			if(Log.isDebugEnabled()) {
				Log.debug("PROXY protocol directive enabled by remote IP adresss {}", remoteAddress);
			}
		}
		
		try {
			if(Log.isInfoEnabled()) {
				Log.info(String.format("Parsing PROXY protocol string [%s]", tmp));
			}
			
			String[] elements = tmp.split(" ");
			if(elements.length < 6) {
					if(Log.isInfoEnabled()) {
						Log.info("Not enough parameters in PROXY statement");
					}
					return;
			}
			
			if(!"TCP4".equals(elements[1]) && !"TCP6".equals(elements[1])) {
				if(Log.isInfoEnabled()) {
					Log.info("Unsupported TCP element {} in PROXY statement", elements[1]);
				}
				return;	
			}
	
			String sourceAddress = elements[2].trim();
			String targetAddress = elements[3].trim();
			int sourcePort = Integer.parseInt(elements[4].trim());
			int targetPort = Integer.parseInt(elements[5].trim());
			
			con.setRemoteAddress(InetSocketAddress.createUnresolved(sourceAddress, sourcePort));
			con.setLocalAddress(InetSocketAddress.createUnresolved(targetAddress, targetPort));

		} catch (Throwable e) {
			Log.error("Failed to parse proxy protocol", e);
			return;
		}

		if(!policy.getIPPolicy().checkConnection(con.getRemoteIPAddress(), con.getLocalIPAddress())) {
			throw new IOException(String.format("IP addresss is not allowed by Load Balancer IP Policy remoteAddress=%s localAddress=%s", 
					con.getRemoteIPAddress(),
					con.getLocalIPAddress()));
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

		boolean canConnect = sshContext.getPolicy(IPPolicy.class).checkConnection(
				((InetSocketAddress)connection.getRemoteAddress()).getAddress(),
				((InetSocketAddress)connection.getLocalAddress()).getAddress());

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
	
	protected void onKeyExchangeComplete() {
		if(hasExtensionCapability && enableExtensionCapability) {
			sendExtensionInfo();
		}
	}
	
	private void sendExtensionInfo() {
		
//		if(AdaptiveConfiguration.getBoolean("disableExtensionInfo", false, con.getRemoteAddress(), con.getIdent())) {
//			return;
//		}
		
		final ByteArrayWriter msg = new ByteArrayWriter();
		
		try {
			msg.writeInt(1);
			msg.writeString("server-sig-algs");
			if(getContext().isSHA1SignaturesSupported()) {
				msg.writeString(getContext().supportedPublicKeys().list(""));
			} else {
				msg.writeString(getContext().supportedPublicKeys().list("", SshContext.PUBLIC_KEY_SSHRSA));
			}
			
			postMessage(new SshMessage() {
				public boolean writeMessageIntoBuffer(ByteBuffer buf) {
					buf.put((byte) TransportProtocol.SSH_MSG_EXT_INFO);
					buf.put(msg.toByteArray());
					return true;
				}

				public void messageSent(Long sequenceNo) {

					if (Log.isDebugEnabled()) {
						Log.debug("Sent SSH_MSG_EXT_INFO");
					}	
				}
			});
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} finally {
			try {
				msg.close();
			} catch (IOException e) {
			}
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

	@Override
	protected String getExtensionNegotiationString() {
		return "ext-info-s";
	}

	@Override
	protected boolean isExtensionNegotiationSupported() {
		return false;
	}

}
