package com.sshtools.client;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.ConnectRequestFuture;
import com.sshtools.common.nio.LicenseException;
import com.sshtools.common.nio.SocketConnection;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.ConnectionTaskWrapper;
import com.sshtools.common.ssh.Service;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.TransportProtocol;
import com.sshtools.common.ssh.components.SshKeyExchange;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.sshd.SshMessage;

//#ifdef LICENSE
//import com.sshtools.common.nio.LicenseManager;
//#endif

/**
 * The implementation of the client side of the SSH transport protocol.
 */
public class TransportProtocolClient extends TransportProtocol<SshClientContext> {

	
	
	Service pendingService; 
	
	//#ifdef LICENSE
	//static final LicenseVerification license = new LicenseVerification();
	//#endif
	public TransportProtocolClient(SshClientContext sshContext, ConnectRequestFuture connectFuture) throws LicenseException {
		super(sshContext,connectFuture);
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

	@Override
	protected boolean canConnect(SocketConnection connection) {
		return true;
	}

	@Override
	protected void initializeKeyExchange(SshKeyExchange<SshClientContext> keyExchange, boolean firstPacketFollows,
			boolean useFirstPacket) throws IOException, SshException {
	
		keyExchange.init(this, localIdentification.toString().trim(),
				remoteIdentification.toString().trim(), localkex, remotekex,
				null, null,
				firstPacketFollows, useFirstPacket);
	}

	@Override
	protected void completeKeyExchange(SshKeyExchange<SshClientContext> keyExchange) {

		try {
			
			SshPublicKey hostkey = SshPublicKeyFileFactory.decodeSSH2PublicKey(keyExchange.getHostKey());
			
			if(getContext().getHostKeyVerification()!=null) {
				
				String host = getConnectFuture().getHost();
				
				if(!Boolean.getBoolean("maverick.knownHosts.disablePortValidate")) {
					if (getConnectFuture().getPort() != 22) {
						host = "[" + host + "]:" + getConnectFuture().getPort();
					}
				}
				
				if (!getContext().getHostKeyVerification()
						.verifyHost(host, hostkey)) {
					EventServiceImplementation
							.getInstance()
							.fireEvent(
									new Event(
											this,
											EventCodes.EVENT_HOSTKEY_REJECTED,
											false)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											getConnection())
									.addAttribute(EventCodes.ATTRIBUTE_HOST_KEY,
											new String(keyExchange.getHostKey()))
									.addAttribute(EventCodes.ATTRIBUTE_HOST_PUBLIC_KEY,
													hostkey));
					disconnect(
							TransportProtocol.HOST_KEY_NOT_VERIFIABLE,
							"Host key not accepted");
					throw new SshException(
							"The host key was not accepted",
							SshException.CANCELLED_CONNECTION);
				}
	
				if (!hostkey.verifySignature(
						keyExchange.getSignature(),
						keyExchange.getExchangeHash())) {
					EventServiceImplementation
							.getInstance()
							.fireEvent(
									new Event(this, EventCodes.EVENT_HOSTKEY_REJECTED, false)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											getConnection())
									.addAttribute(EventCodes.ATTRIBUTE_HOST_KEY, new String(keyExchange.getHostKey()))
									.addAttribute(EventCodes.ATTRIBUTE_HOST_PUBLIC_KEY, hostkey));
					disconnect(
							TransportProtocol.HOST_KEY_NOT_VERIFIABLE,
							"Invalid host key signature");
	
					throw new SshException(
							"The host key signature is invalid",
							SshException.PROTOCOL_VIOLATION);
				}
			}
			
			EventServiceImplementation.getInstance().fireEvent(
					new Event(this,
							EventCodes.EVENT_HOSTKEY_ACCEPTED,
							true).addAttribute(
									EventCodes.ATTRIBUTE_CONNECTION,
									getConnection())
					             .addAttribute(EventCodes.ATTRIBUTE_HOST_KEY, new String(keyExchange.getHostKey()))
								.addAttribute(EventCodes.ATTRIBUTE_HOST_PUBLIC_KEY, hostkey));
		
			boolean first = !completedFirstKeyExchange;
			super.completeKeyExchange(keyExchange);
			sshContext.keysExchanged(first);
			
		} catch (SshException | IOException e) {
			if(Log.isErrorEnabled()) {
				Log.error("Could not verify host key", e);
			}
			disconnect(HOST_KEY_NOT_VERIFIABLE, "The host key could not be verified.");
		}
		
	}
	@Override
	protected void onNewKeysReceived() {
		generateNewKeysClientIn();
	}
	
	@Override
	protected void onNewKeysSent() {
		generateNewKeysClientOut();
	}

	@Override
	protected String selectNegotiatedComponent(String remotelist, String locallist)
			throws IOException {
		/**
		 * This switches the lists around so that we are selected identically to the
		 * server
		 */
		return super.selectNegotiatedComponent(locallist, remotelist);
	}
	
	@Override
	protected boolean processTransportMessage(int msgid, byte[] msg)
			throws IOException {
		
		switch(msgid) {
		case SSH_MSG_SERVICE_ACCEPT:
			if(pendingService!=null) {
				activeService = pendingService;
				pendingService = null;
				activeService.start();
			}
			return true;
		}
		return false;
	}

	public void setActiveService(Service service) {
		this.activeService = service;
	}
	
	public Service getActiveService() {
		return this.activeService;
	}
	
	public void startService(final Service service) {
		
		pendingService = service;
		
		postMessage(new SshMessage() {

			byte[] serviceNameBytes = getBytes(service.getName(), CHARSET_ENCODING);

			@Override
			public boolean writeMessageIntoBuffer(ByteBuffer buf) {
				
				buf.put((byte)SSH_MSG_SERVICE_REQUEST);
				buf.putInt(serviceNameBytes.length);
				buf.put(serviceNameBytes);
				
				return true;
			}

			@Override
			public void messageSent(Long sequenceNo) {
				if(Log.isDebugEnabled()) {
					Log.debug("Sent SSH_MSG_SERVICE_REQUEST %s", service);
				}
			}
		});
	}

	@Override
	protected void disconnected() {
		
		addTask(EVENTS, new ConnectionTaskWrapper(getConnection(), new Runnable() {
			public void run() {
				for(ClientStateListener stateListener : sshContext.getStateListeners()) {
					stateListener.disconnected(con);
				}
			}
		}));
		
	}

	@Override
	protected void onConnected() {
		getContext().getConnectionManager().registerTransport(this, sshContext);
	}

	@Override
	protected void onDisconnected() {
		getContext().getConnectionManager().unregisterTransport(this);
	}
	
	public String getName() {
		return "transport-client";
	}

	@Override
	protected SocketAddress getConnectionAddress() {
		return getLocalAddress();
	}
}
