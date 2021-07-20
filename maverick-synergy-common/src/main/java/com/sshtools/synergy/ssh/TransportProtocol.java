

package com.sshtools.synergy.ssh;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.nio.IdleStateListener;
import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.ExecutorOperationQueues;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.SshHmac;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.ChaCha20Poly1305;
import com.sshtools.common.ssh.compression.SshCompression;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger64;
import com.sshtools.common.util.Utils;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.nio.DisconnectRequestFuture;
import com.sshtools.synergy.nio.ProtocolEngine;
import com.sshtools.synergy.nio.SocketConnection;
import com.sshtools.synergy.nio.SocketWriteCallback;
import com.sshtools.synergy.ssh.components.SshKeyExchange;

/**
 * This class implements the SSH Transport Protocol for the SSHD server.
 */
public abstract class TransportProtocol<T extends SshContext> 
		extends ExecutorOperationSupport<SshContext> 
			implements ProtocolEngine, IdleStateListener, SshTransport<T> {

	/**
	 * Character set encoding. All input/output strings created by the API are
	 * created with this encoding. The default is "UTF-8" and it may be changed,
	 * the results however are unknown.
	 */
	public static String CHARSET_ENCODING = "UTF-8";

	SecureRandom rnd = new SecureRandom();
	
	byte[] incomingSwap;
	
	protected String localIdentification = "SSH-2.0-";
	protected StringBuffer remoteIdentification = new StringBuffer();
	protected boolean receivedRemoteIdentification = false;
	protected boolean sentLocalIdentification = false;
	boolean postedIdentification = false;
	protected byte[] localkex;
	protected byte[] remotekex;
	protected byte[] sessionIdentifier;
	protected UUID uuid;

	LinkedList<SshMessage> outgoingQueue = new LinkedList<SshMessage>();
	LinkedList<SshMessage> kexQueue = new LinkedList<SshMessage>();

	protected Service activeService;
	List<TransportProtocolListener> transportListeners = new ArrayList<>();
	List<IdleStateListener> idleListeners = new ArrayList<>();
	
	// Message type numbers
	static final int SSH_MSG_DISCONNECT = 1;
	static final int SSH_MSG_IGNORE = 2;
	static final int SSH_MSG_UNIMPLEMENTED = 3;
	static final int SSH_MSG_DEBUG = 4;
	protected static final int SSH_MSG_SERVICE_REQUEST = 5;
	public static final int SSH_MSG_SERVICE_ACCEPT = 6;

	static final int SSH_MSG_KEX_INIT = 20;
	static final int SSH_MSG_NEWKEYS = 21;

	// Message processing variables
	boolean expectPacket = true;
	int expectedBytes = 0;
	byte[] payloadIncoming;
	byte[] packet;
	int offsetIncoming = 0;

	int numOutgoingBytesSinceKEX;
	int numOutgoingPacketsSinceKEX;
	int numIncomingBytesSinceKEX;
	int numIncomingPacketsSinceKEX;

	long lastActivity = System.currentTimeMillis();
	boolean closed = false;
	
	protected boolean completedFirstKeyExchange = false;
	protected Date disconnectStarted = null;
	
	protected void transferState(TransportProtocol<? extends SshContext> transport) {
		
		transport.localIdentification = localIdentification;
		transport.remoteIdentification = remoteIdentification;
		transport.receivedRemoteIdentification = true;
		transport.sentLocalIdentification = true;
		transport.sessionIdentifier = sessionIdentifier;
		transport.uuid = uuid;
		transport.currentState = currentState;
		transport.lastActivity = lastActivity;
		transport.outgoingQueue.addAll(outgoingQueue);
		transport.kexQueue.addAll(kexQueue);
		transport.socketConnection = socketConnection;
		transport.postedIdentification = postedIdentification;
		transport.onSocketConnect(socketConnection);
		
		receivedRemoteIdentification = false;
		currentState = DISCONNECTED;

	}
	
	public ConnectRequestFuture getConnectFuture() {
		return connectFuture;
	}
	
	public DisconnectRequestFuture getDisconnectFuture() {
		return disconnectFuture;
	}
	
	/**
	 * Protocol state: Negotation of the protocol version
	 */
	public final static int NEGOTIATING_PROTOCOL = 1;

	/**
	 * Protocol state: The protocol is performing key exchange
	 */
	public final static int PERFORMING_KEYEXCHANGE = 2;

	/**
	 * Protocol state: The transport protocol is connected and services can be
	 * started or may already be active.
	 */
	public final static int CONNECTED = 3;

	/**
	 * Protocol state: The transport protocol has disconnected.
	 * 
	 * @see #getLastError()
	 */
	public final static int DISCONNECTED = 4;

	int currentState = TransportProtocol.NEGOTIATING_PROTOCOL;
	SshKeyExchange<T> keyExchange;
	SshCipher encryption;
	SshCipher decryption;
	SshHmac outgoingMac;
	SshHmac incomingMac;
	SshCompression outgoingCompression;
	SshCompression incomingCompression;
	
	protected SshPublicKey hostKey;
	
	// C=Client
	// S=Server
	protected String cipherCS;
	protected String cipherSC;
	protected String macCS;
	protected String macSC;
	protected String compressionCS;
	protected String compressionSC;
	protected String keyExchangeAlgorithm;
	protected String publicKey;
	
	String remoteKeyExchanges;
	String remotePublicKeys;
	String remoteCiphersCS;
	String remoteCiphersSC;
	String remoteCSMacs;
	String remoteSCMacs;
	String remoteCSCompressions;
	String remoteSCCompressions;
	
	long outgoingSequence = 0;
	long incomingSequence = 0;

	long outgoingBytes = 0;
	long incomingBytes = 0;

	Object kexlockIn = new Object();
	Object kexlockOut = new Object();
	
	boolean queuedKexInit = false;
	boolean sentKexInit = false;

	protected Connection<T> con;
	
	/** Disconnect reason: The host is not allowed */
	public final static int HOST_NOT_ALLOWED = 1;

	/** Disconnect reason: A protocol error occurred */
	public final static int PROTOCOL_ERROR = 2;

	/** Disconnect reason: Key exchange failed */
	public final static int KEY_EXCHANGE_FAILED = 3;

	/** Disconnect reason: Reserved */
	public final static int RESERVED = 4;

	/** Disconnect reason: An error occurred verifying the MAC */
	public final static int MAC_ERROR = 5;

	/** Disconnect reason: A compression error occurred */
	public final static int COMPRESSION_ERROR = 6;

	/** Disconnect reason: The requested service is not available */
	public final static int SERVICE_NOT_AVAILABLE = 7;

	/** Disconnect reason: The protocol version is not supported */
	public final static int PROTOCOL_VERSION_NOT_SUPPORTED = 8;

	/** Disconnect reason: The host key supplied could not be verified */
	public final static int HOST_KEY_NOT_VERIFIABLE = 9;

	/** Disconnect reason: The connection was lost */
	public final static int CONNECTION_LOST = 10;

	/** Disconnect reason: The application disconnected */
	public final static int BY_APPLICATION = 11;

	/** Disconnect reason: Too many connections, try later */
	public final static int TOO_MANY_CONNECTIONS = 12;

	/** Disconnect reason: Authentication was cancelled */
	public final static int AUTH_CANCELLED_BY_USER = 13;

	/** Disconnect reason: No more authentication methods are available */
	public final static int NO_MORE_AUTH_METHODS_AVAILABLE = 14;

	/** Disconnect reason: The user's name is illegal */
	public final static int ILLEGAL_USER_NAME = 15;

	private static final Integer ACTIVE_SERVICE_IN = ExecutorOperationQueues.generateUniqueQueue("TransportProtocol.activeService.in");

	IgnoreMessage ignoreMessage;
	long lastKeepAlive = 0;

	protected T sshContext;
	protected SocketConnection socketConnection;
	public static Object lock = new Object();
	Date started = new Date();
	ConnectRequestFuture connectFuture;
	DisconnectRequestFuture disconnectFuture = new DisconnectRequestFuture();
	
	/**
	 * Create a default transport protocol instance in CLIENT_MODE.
	 * 
	 * @throws IOException
	 */
	public TransportProtocol(T sshContext, ConnectRequestFuture connectFuture) {
		super("transport-protocol");
		this.sshContext = sshContext;
		this.ignoreMessage = new IgnoreMessage();
		this.connectFuture = connectFuture;
		this.uuid = UUID.randomUUID();
		this.incomingSwap = new byte[sshContext.getMaximumPacketLength()];
	}

	public SocketConnection getSocketConnection() {
		return socketConnection;
	}

	public void addEventListener(TransportProtocolListener listener) {
		if (listener != null)
			transportListeners.add(listener);
	}

	public SocketAddress getRemoteAddress() {
		return socketConnection.getRemoteAddress();
	}

	/**
	 * Returns the remote port of the connected socket.
	 * 
	 * @return int
	 */
	public int getRemotePort() {
		return socketConnection.getPort();
	}

	public T getContext() {
		return sshContext;
	}
	
	public Connection<T> getConnection() {
		return con;
	}
	protected abstract boolean canConnect(SocketConnection connection);
	
	protected abstract void onConnected();
	
	protected abstract void onDisconnected();
	
	public void onSocketConnect(SocketConnection connection) {

		this.socketConnection = connection;

		if(Log.isInfoEnabled()) {
			Log.info("Connnection created {} on interface {}", 
					socketConnection.getRemoteAddress().toString(),
					socketConnection.getLocalAddress().toString());
		}
		
		if (!canConnect(connection)) {
			if(Log.isDebugEnabled())
				Log.debug("Access denied by TransportProtocol.canConnect");
			
			
			EventServiceImplementation.getInstance().fireEvent(
					(new Event(this, EventCodes.EVENT_CONNECTED, new IOException("Access Denied")))
							.addAttribute(
									EventCodes.ATTRIBUTE_CONNECTION,
									con)
							.addAttribute(
									EventCodes.ATTRIBUTE_OPERATION_STARTED,
									started)
							.addAttribute(
									EventCodes.ATTRIBUTE_OPERATION_FINISHED,
									new Date()));
			
			connection.closeConnection(false);
			
			return;
		}
		
		connection.getIdleStates().register(this);
		onConnected();

		if (!sentLocalIdentification) {
		
			EventServiceImplementation.getInstance().fireEvent(
					(new Event(this, EventCodes.EVENT_CONNECTED, true))
							.addAttribute(
									EventCodes.ATTRIBUTE_CONNECTION,
									con)
							.addAttribute(
									EventCodes.ATTRIBUTE_OPERATION_STARTED,
									started)
							.addAttribute(
									EventCodes.ATTRIBUTE_OPERATION_FINISHED,
									new Date()));
			
			this.localIdentification += sshContext.getSoftwareVersionComments() + "\r\n";
			// Send our identification String
			
			if(!sshContext.isHttpRedirect()) {
				sendLocalIdentification(false, null);
			}
		}

	}
	
	private synchronized void sendLocalIdentification() {
		sendLocalIdentification(false, null);
	}
	
	private synchronized void sendLocalIdentification(final boolean doHttpRedirect, final String hostname) {
		
		if(!postedIdentification) {
			postedIdentification = true;
	  		postMessage(new SshMessage() {
				public boolean writeMessageIntoBuffer(ByteBuffer buf) {
					
					try {
						if(doHttpRedirect){
							String httpRedirect = "HTTP/1.1 302 Moved Location\r\n"
									+ "Location: " + sshContext.getHttpRedirectUrl().replace("${hostname}", hostname) + "/\r\n"
									+ "Connection: close\r\n"
									+ "Content-Type: text/plain\r\n"
									+ "Content-Length: " + localIdentification.getBytes("UTF-8").length + "\r\n\r\n";
							buf.put(httpRedirect.getBytes("UTF-8"));
						}
						buf.put(localIdentification.getBytes("UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new IllegalStateException("UTF-8 is not supported!!");
					}
					return true;
				}
	
				public void messageSent(Long sequenceNo) {
					if(Log.isDebugEnabled())
						Log.debug("Sent local identification string "
								+ localIdentification.trim());
	
					sentLocalIdentification = true;
	
					if (receivedRemoteIdentification && canSendKeyExchangeInit())
						sendKeyExchangeInit();
				}
			});
		}
	}

	protected boolean canSendKeyExchangeInit() {
		return true;
	}
	
	/**
	 * Called when the socket channel is reported to be ready for reading.
	 * 
	 */
	public boolean onSocketRead(ByteBuffer incomingData) {

		if(Log.isTraceEnabled())
			Log.trace("Processing APPLICATION READ data");

		boolean wantsWrite = false;

		try {

			// What's the protocol's state
			switch (currentState) {
			case TransportProtocol.NEGOTIATING_PROTOCOL:
				negotiateProtocol(incomingData);
				break;
			case TransportProtocol.PERFORMING_KEYEXCHANGE:
			case TransportProtocol.CONNECTED:
				wantsWrite = processBinaryPackets(incomingData);
				break;
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
			if(Log.isInfoEnabled()) {
				Log.info("Read error from {} {}", 
						getConnectionAddress().toString(),
						ex.getMessage());
			}
			if(Log.isDebugEnabled())
				Log.debug("Connection closed on socket read", ex);
			socketConnection.closeConnection();
		}

		return wantsWrite;
	}

	/**
	 * Determine if the protocol is still connected
	 * 
	 * @return boolean
	 */
	public boolean isConnected() {
		return (currentState == NEGOTIATING_PROTOCOL
				|| currentState == PERFORMING_KEYEXCHANGE || currentState == CONNECTED);
	}

	/**
	 * Negotiate the protocol version with the client
	 * 
	 * @throws IOException
	 */
	void negotiateProtocol(ByteBuffer applicationData) throws IOException {

		if (receivedRemoteIdentification) {
			processBinaryPackets(applicationData);
			return;
		}

		char c = 0;

		while (applicationData.remaining() > 0) {
			c = (char) applicationData.get();

			if (c == '\n') {
				break;
			}
			remoteIdentification.append(c);
		}

		if (c == '\n' && remoteIdentification.length() > 4 
				&& remoteIdentification.charAt(0) == 'S'
				&& remoteIdentification.charAt(1) == 'S'
				&& remoteIdentification.charAt(2) == 'H'
				&& remoteIdentification.charAt(3) == '-') {

			if(Log.isInfoEnabled()) {
				Log.info("Connnection {} identifies itself as {}", 
						getConnectionAddress().toString(),
						remoteIdentification.toString().trim());
			}

			sendLocalIdentification(false, null);
			
			// Check the remote client version
			String tmp = remoteIdentification.toString();

			if (!tmp.startsWith("SSH-2.0-") && !tmp.startsWith("SSH-1.99-")) {
				if(Log.isDebugEnabled())
					Log.debug("Remote client reported an invalid protocol version!");
				socketConnection.closeConnection();
				return;
			}
			
			if(Log.isDebugEnabled())
				Log.debug("Remote client version OK");

			receivedRemoteIdentification = true;

			EventServiceImplementation.getInstance().fireEvent(
					(new Event(this, EventCodes.EVENT_NEGOTIATED_PROTOCOL, true))
							.addAttribute(
									EventCodes.ATTRIBUTE_CONNECTION,
									con)
							.addAttribute(
									EventCodes.ATTRIBUTE_OPERATION_STARTED,
									started)
							.addAttribute(
									EventCodes.ATTRIBUTE_OPERATION_FINISHED,
									new Date()));
			
			onRemoteIdentificationReceived(tmp);
			
			// Send our kex init
			if (sentLocalIdentification) {
				if(canSendKeyExchangeInit()) {
					sendKeyExchangeInit();
				}

				// Make sure that any remaining data is
				// processed by the binary packet protocol
				processBinaryPackets(applicationData);
			}
			
			return;
		}
		
		if(sshContext.isHttpRedirect()) {
			String line = remoteIdentification.toString();
			if(line.startsWith("Host:")) {
				String hostname = line.substring(5).trim();
				if(hostname.contains(":")) {
					hostname = hostname.substring(0, hostname.indexOf(':'));
				}
				sendLocalIdentification(true, hostname);
			}
		}
		
		remoteIdentification.setLength(0);
		
		if(applicationData.hasRemaining()) {
			negotiateProtocol(applicationData);
		}
	}
	
	protected void onRemoteIdentificationReceived(String remoteIdentification) {
		
	}

	/**
	 * Process data into actual SSH messages
	 */
	boolean processBinaryPackets(ByteBuffer applicationData) {

		boolean requiresWriteOperation = false;
		boolean hasMessage = false;

		try {

			while (isConnected()
					&& ((expectPacket && (applicationData.remaining() > incomingCipherLength)) || (expectedBytes > 0 && applicationData
							.hasRemaining())) && !requiresWriteOperation) {

				/**
				 * Lock the key exchange variables, we do not want to change
				 * these whilst we are decrypting and authenticating a message.
				 * We will release this before we process the message to ensure
				 * other threads do not get locked up sending data.
				 */
				synchronized (kexlockIn) {

					if(decryption!=null && decryption instanceof ChaCha20Poly1305) {
						hasMessage = decodeChaCha20Poly1305Format(applicationData);
					} else if(incomingMac!=null && incomingMac.isETM()) {
						hasMessage = decodeETMPacketFormat(applicationData);
					} else {
						hasMessage = decodeOriginalPacketFormat(applicationData);
					}
					
				}

				/**
				 * Process the message outside of kexlock. This is to ensure
				 * that any thread attempting to send a message on this
				 * connection can still send without causing a lockup.
				 */
				if (hasMessage) {
					// Process the message
					try {
						processMessage(payloadIncoming, incomingSequence);
					} catch (WriteOperationRequest x) {
						requiresWriteOperation = true;
					} finally {
						// Update stats and sequence
						if (++incomingSequence >= 4294967296L) {
							incomingSequence = 0;
						}

						incomingBytes += payloadIncoming.length;

						numIncomingBytesSinceKEX += payloadIncoming.length;
						numIncomingPacketsSinceKEX++;

						// if done alot of communication then change keys
						if (numIncomingBytesSinceKEX >= getContext()
								.getKeyExchangeTransferLimit()
								|| numIncomingPacketsSinceKEX >= getContext()
										.getKeyExchangePacketLimit()) {
							sendKeyExchangeInit();
						}

						// Reset variables for a new message
						expectPacket = true;
						expectedBytes = 0;
						offsetIncoming = 0;
						payloadIncoming = null;
						hasMessage = false;
					}
				}
			}

			if(Log.isTraceEnabled())
				Log.trace("Transport protocol "
						+ (expectPacket ? "is expecting another packet"
								: "still has "
										+ expectedBytes
										+ " bytes of data to complete packet with "
										+ offsetIncoming
										+ " bytes already received"
										+ " requiresWrite="
										+ requiresWriteOperation));

		} catch (Throwable ex) {
			ex.printStackTrace();
			if(Log.isInfoEnabled()) {
				Log.info("Transport error {} {}", 
						getConnectionAddress().toString(),
						ex.getMessage());
			}
			if(Log.isDebugEnabled())
				Log.debug("Connection Error", ex);
			if (isConnected())
				disconnect(TransportProtocol.PROTOCOL_ERROR,
						"The application encountered an error");
			requiresWriteOperation = true;
		}

		return requiresWriteOperation;

	}
	
	private boolean decodeChaCha20Poly1305Format(ByteBuffer applicationData) throws IOException {
		
		ChaCha20Poly1305 cipher = (ChaCha20Poly1305) decryption;
		
		if (expectPacket) {

			/**
			 * We need to decrypt the initial binary packet header
			 * to determine how much data we are expecting
			 */
			applicationData.get(incomingSwap, 0, 4);

			// Work out the message length, payload, padding and
			// remaining bytes
			msglen = (int) cipher.readPacketLength(incomingSwap, new UnsignedInteger64(incomingSequence));

			if (msglen <= 0)
				throw new IOException(
						"Client sent invalid message length of "
								+ msglen + "!");
			
			if ((msglen + 4) < 0
					|| (msglen + 4) > sshContext
							.getMaximumPacketLength()) {
				disconnect(
						TransportProtocol.PROTOCOL_ERROR,
						"Incoming packet length "
								+ msglen
								+ ((msglen + 4) < 0 ? " is too small"
										: " exceeds maximum supported length of "
												+ sshContext
														.getMaximumPacketLength()));
				throw new IOException("Disconnected");
			}

			
			remaining = msglen;
			expectedBytes = remaining + incomingMacLength;
			expectPacket = false;
			offsetIncoming += 4;
			
		}
		
		/**
		 * If we have more data to get from the network do it now
		 */
		if (!expectPacket && applicationData.remaining() > 0) {

			/**
			 * Determine how many bytes to process this time
			 */
			int count = (expectedBytes > applicationData
					.remaining() ? applicationData.remaining()
					: expectedBytes);

			/**
			 * Now get the data
			 */
			applicationData.get(incomingSwap, offsetIncoming, count);

			/**
			 * Update our position in the swap buffer and the number
			 * of expected bytes
			 */
			expectedBytes -= count;
			offsetIncoming += count;

			/**
			 * Only complete the message once we have all the bytes
			 */
			if (expectedBytes == 0) {
				
				// Record the packet legth
//				int packetlen = msglen;

				decryption.transform(incomingSwap,
							4, incomingSwap,
							4, remaining + incomingMacLength);

				
				
				padlen = (incomingSwap[4] & 0xFF);
				payloadIncoming = new byte[msglen - padlen - 1];

				// Copy the payload into the final output buffer
				System.arraycopy(incomingSwap, 5, payloadIncoming,
						0, msglen - padlen - 1);

				// Uncompress the message payload if necersary
				if (incomingCompression != null) {
					payloadIncoming = incomingCompression
							.uncompress(payloadIncoming, 0,
									payloadIncoming.length);
				}

				return true;

			}
		}
		
		return false;
		
	}

	private boolean decodeETMPacketFormat(ByteBuffer applicationData) throws IOException {
		
		if (expectPacket) {

			/**
			 * We need to decrypt the initial binary packet header
			 * to determine how much data we are expecting
			 */
			applicationData.get(incomingSwap, offsetIncoming,
					4);

			// Work out the message length, payload, padding and
			// remaining bytes
			msglen = (int) ByteArrayReader.readInt(incomingSwap,
					0);

			if (msglen <= 0)
				throw new IOException(
						"Client sent invalid message length of "
								+ msglen + "!");

			if ((msglen + 4) < 0
					|| (msglen + 4) > sshContext
							.getMaximumPacketLength()) {
				disconnect(
						TransportProtocol.PROTOCOL_ERROR,
						"Incoming packet length "
								+ msglen
								+ ((msglen + 4) < 0 ? " is too small"
										: " exceeds maximum supported length of "
												+ sshContext
														.getMaximumPacketLength()));
				throw new IOException("Disconnected");
			}

			
			remaining = msglen;
			expectedBytes = remaining + incomingMacLength;
			expectPacket = false;
			offsetIncoming += 4;

		}

		/**
		 * If we have more data to get from the network do it now
		 */
		if (!expectPacket && applicationData.remaining() > 0) {

			/**
			 * Determine how many bytes to process this time
			 */
			int count = (expectedBytes > applicationData
					.remaining() ? applicationData.remaining()
					: expectedBytes);

			/**
			 * Now get the data
			 */
			applicationData
					.get(incomingSwap, offsetIncoming, count);

			/**
			 * Update our position in the swap buffer and the number
			 * of expected bytes
			 */
			expectedBytes -= count;
			offsetIncoming += count;

			/**
			 * Only complete the message once we have all the bytes
			 */
			if (expectedBytes == 0) {

				// Record the packet legth
				int packetlen = msglen;

				// Verify the message
				if (incomingMac != null) {
					if (!incomingMac.verify(incomingSequence,
							incomingSwap, 0, packetlen+4,
							incomingSwap, packetlen+4)) {
						throw new IOException(
								"Corrupt Mac on input");
					}
				}
				
				// Decrypt the data now that we have it all
				if (decryption != null) {
					decryption.transform(incomingSwap,
							4, incomingSwap,
							4, remaining);

				}
				
				padlen = (incomingSwap[4] & 0xFF);
				payloadIncoming = new byte[msglen - padlen - 1];

				// Copy the payload into the final output buffer
				System.arraycopy(incomingSwap, 5, payloadIncoming,
						0, msglen - padlen - 1);

				// Uncompress the message payload if necersary
				if (incomingCompression != null) {
					payloadIncoming = incomingCompression
							.uncompress(payloadIncoming, 0,
									payloadIncoming.length);
				}

				return true;

			}
		}
		
		return false;
	}

	private boolean decodeOriginalPacketFormat(ByteBuffer applicationData) throws IOException {
	
		
		if (expectPacket) {

			/**
			 * We need to decrypt the initial binary packet header
			 * to determine how much data we are expecting
			 */
			applicationData.get(incomingSwap, offsetIncoming,
					incomingCipherLength);

			if (decryption != null && !decryption.isMAC()) {
				decryption.transform(incomingSwap, offsetIncoming,
						incomingSwap, offsetIncoming,
						incomingCipherLength);
			}

			// Work out the message length, payload, padding and
			// remaining bytes
			msglen = (int) ByteArrayReader.readInt(incomingSwap,
					offsetIncoming);

			if (msglen <= 0)
				throw new IOException(
						"Client sent invalid message length of "
								+ msglen + "!");

			if ((msglen + 4) < 0
					|| (msglen + 4) > sshContext
							.getMaximumPacketLength()) {
				disconnect(
						TransportProtocol.PROTOCOL_ERROR,
						"Incoming packet length "
								+ msglen
								+ ((msglen + 4) < 0 ? " is too small"
										: " exceeds maximum supported length of "
												+ sshContext
														.getMaximumPacketLength()));
				throw new IOException("Disconnected");
			}

			padlen = (incomingSwap[4] & 0xFF);
			remaining = (msglen - (incomingCipherLength - 4));
			expectedBytes = remaining + incomingMacLength;

			// Create our final storage buffer for the decrpyted
			// message
			expectPacket = false;
			offsetIncoming += incomingCipherLength;

		}

		/**
		 * If we have more data to get from the network do it now
		 */
		if (!expectPacket && applicationData.remaining() > 0) {

			/**
			 * Determine how many bytes to process this time
			 */
			int count = (expectedBytes > applicationData
					.remaining() ? applicationData.remaining()
					: expectedBytes);

			/**
			 * Now get the data
			 */
			applicationData
					.get(incomingSwap, offsetIncoming, count);

			/**
			 * Update our position in the swap buffer and the number
			 * of expected bytes
			 */
			expectedBytes -= count;
			offsetIncoming += count;

			/**
			 * Only complete the message once we have all the bytes
			 */
			if (expectedBytes == 0) {

				// Record the packet legth
				int packetlen = msglen + 4;

				// Decrypt the data now that we have it all
				if (decryption != null) { 
					if(!decryption.isMAC()) {
					decryption.transform(incomingSwap,
							incomingCipherLength, incomingSwap,
							incomingCipherLength, remaining);
					} else {
						decryption.transform(incomingSwap, 0, incomingSwap, 0, msglen + 4 + decryption.getMacLength());
						padlen = (incomingSwap[4] & 0xFF);
					}
				} 

				// Verify the message
				if (incomingMac != null) {
					if (!incomingMac.verify(incomingSequence,
							incomingSwap, 0, packetlen,
							incomingSwap, packetlen)) {
						throw new IOException(
								"Corrupt Mac on input");
					}
				}

				payloadIncoming = new byte[msglen - padlen - 1];
				
				// Copy the payload into the final output buffer
				System.arraycopy(incomingSwap, 5, payloadIncoming,
						0, msglen - padlen - 1);

				// Uncompress the message payload if necersary
				if (incomingCompression != null) {
					payloadIncoming = incomingCompression
							.uncompress(payloadIncoming, 0,
									payloadIncoming.length);
				}

				return true;

			}
		}
		
		return false;
	}

	public boolean wantsToWrite() {
		synchronized (kexlockOut) {
			if (currentState == PERFORMING_KEYEXCHANGE
					&& completedFirstKeyExchange) {
				return kexQueue.size() > 0;
			}
			return outgoingQueue.size() > 0 || kexQueue.size() > 0;
		}
	}

	public int getQueueSizes() {
		synchronized (kexlockOut) {
			return outgoingQueue.size() + kexQueue.size();
		}
	}

	/**
	 * Called when the selector framework is idle. We take the opportunity to
	 * send an SSH_MSG_IGNORE message in the hope that we can detect any sockets
	 * that may have closed.
	 */
	public boolean idle() {

		if (currentState == TransportProtocol.DISCONNECTED)
			return true; // Remove from idle state manager

		long idleTimeSeconds = (System.currentTimeMillis() - lastActivity) / 1000;

		if(currentState == NEGOTIATING_PROTOCOL 
				|| currentState == PERFORMING_KEYEXCHANGE) {
			if(con.getContext().getIdleAuthenticationTimeoutSeconds() < idleTimeSeconds) {
				if(Log.isDebugEnabled()) {
					Log.debug("Idle time of {} seconds exceeded threshold of {} seconds", 
							idleTimeSeconds,
							con.getContext().getIdleAuthenticationTimeoutSeconds());
				}
				disconnect(BY_APPLICATION, "Remote exceeded idle timeout for unauthenticated connections");
				return true;
			}
		}
		if (currentState == TransportProtocol.CONNECTED
				|| currentState == TransportProtocol.PERFORMING_KEYEXCHANGE) {
			if(getContext().isSendIgnorePacketOnIdle()) {
				if (getContext().getKeepAliveInterval() > 0
						&& idleTimeSeconds > getContext().getKeepAliveInterval()) {
	
					long keepAliveSeconds = getContext().getKeepAliveInterval() + 1;
	
					if (lastKeepAlive > 0) {
						keepAliveSeconds = (System.currentTimeMillis() - lastKeepAlive) / 1000;
					}
	
					if (keepAliveSeconds > getContext().getKeepAliveInterval()) {
						postMessage(ignoreMessage);
						lastKeepAlive = System.currentTimeMillis();
					}
				}
			}
		}
		
		
		if (activeService!=null && activeService.getIdleTimeoutSeconds() > 0) {
			if(activeService!=null && idleTimeSeconds >= activeService.getIdleTimeoutSeconds()) {
				return activeService.idle();
			}
		}

		return false;
	}
	
	/**
	 * Called when the socket channel is reported to be ready for writing.
	 */
	public SocketWriteCallback onSocketWrite(ByteBuffer outgoingMessage) {

		if(Log.isTraceEnabled())
			Log.debug("Processing APPLICATION WRITE event");

		final SshMessage msg;

		try {

			final Long sequenceNo = outgoingSequence;
			
			synchronized (kexlockOut) {
				if ((kexQueue.size() > 0 || outgoingQueue.size() > 0)) {

					// Get the next message and write into the buffer
					if (currentState == PERFORMING_KEYEXCHANGE
							&& completedFirstKeyExchange) {
						if (kexQueue.size() > 0) {
							msg = (SshMessage) kexQueue.getFirst();
							if (msg.writeMessageIntoBuffer(outgoingMessage))
								kexQueue.removeFirst();
						} else {
							// Simply return there are no key exchange messages
							// to send
							// socketConnection.setWriteState(wantsToWrite());
							return null;
						}
					} else {
						synchronized (outgoingQueue) {
							msg = (SshMessage) outgoingQueue.getFirst();
							if (msg.writeMessageIntoBuffer(outgoingMessage)) {
								outgoingQueue.removeFirst();
							}
						}
					}

					if (currentState != TransportProtocol.NEGOTIATING_PROTOCOL) {

						outgoingMessage.flip();
						
						if(encryption!=null && encryption instanceof ChaCha20Poly1305) {
							encodeChaCha20Poly1305FormatPacket(outgoingMessage);
						} else if(outgoingMac!=null && outgoingMac.isETM()) {
							encodeETMFormatPacket(outgoingMessage);
						} else {
							encodeOriginalFormatPacket(outgoingMessage);
						}

						numOutgoingBytesSinceKEX += outgoingMessage.position();
						numOutgoingPacketsSinceKEX++;

						outgoingSequence++;

						if (outgoingSequence >= 4294967296L) {
							outgoingSequence = 0;
						}
					}
				} else {
					msg = null;
				}

				// if sent lots of bytes or packets then change keys
				if (numOutgoingBytesSinceKEX >= getContext()
						.getKeyExchangeTransferLimit()
						|| numOutgoingPacketsSinceKEX >= getContext()
								.getKeyExchangePacketLimit()) {
					sendKeyExchangeInit();
				}

				return new SocketWriteCallback() {

					public void completedWrite() {
						
							try {
								if (msg != null) {
									msg.messageSent(sequenceNo);
								}
							} catch (SshException e) {
								Log.error("Failed during messageSent", e);
								disconnect(PROTOCOL_ERROR, "Internal error");
							}
					}
				};
			} // End kexlock
		} catch (Throwable ex) {
			ex.printStackTrace();
			if(Log.isInfoEnabled()) {
				Log.info("Write error from {} {}", 
						getConnectionAddress().toString(),
						ex.getMessage());
			}
			if(Log.isDebugEnabled()) {
				Log.debug("Connection closed on socket write", ex);
			}
			socketConnection.closeConnection();
			return null;
		}

	}
	
	private void encodeChaCha20Poly1305FormatPacket(ByteBuffer outgoingMessage) throws IOException {
		
		ChaCha20Poly1305 cipher = (ChaCha20Poly1305) encryption;
		
		byte[] payload = new byte[outgoingMessage.remaining()];
		outgoingMessage.get(payload);
		outgoingMessage.clear();

		int padding = 4;
		int cipherlen = 8;
		
		// Compress the payload if necersary
		if (outgoingCompression != null) {
			payload = outgoingCompression.compress(payload, 0,
					payload.length);
		}

		// Determine the padding length
		padding += ((cipherlen - ((payload.length + 1 + padding) % cipherlen)) % cipherlen);

		// Write the packet length field
		outgoingMessage.put(cipher.writePacketLength(payload.length + 1 + padding, new UnsignedInteger64(outgoingSequence)));

		// Write the padding length
		outgoingMessage.put((byte) padding);

		// Write the message payload
		outgoingMessage.put(payload, 0, payload.length);
		outgoingBytes += payload.length + padding + 1 + cipher.getMacLength() + 4;

		// Create some random data for the padding
		byte[] pad = new byte[padding];
		rnd.nextBytes(pad);

		// Write the padding
		outgoingMessage.put(pad);

		outgoingMessage.flip();
		
		// Get the unencrypted packet data
		byte[] packet = new byte[outgoingMessage.remaining() + encryption.getMacLength()];
		
		outgoingMessage.get(packet, 0, outgoingMessage.remaining());
		
		cipher.transform(packet, 4, packet, 4, packet.length-4);

		// Reset the message
		outgoingMessage.clear();

		// Write the packet data
		outgoingMessage.put(packet);
		
		
	}	

	private void encodeETMFormatPacket(ByteBuffer outgoingMessage) throws IOException {
		
		/**
		 * Wrap the message payload into the binary packet
		 * format
		 */
		byte[] payload = new byte[outgoingMessage.remaining()];
		outgoingMessage.get(payload);
		outgoingMessage.clear();

		int padding = 4;
		int cipherlen = 8;

		// Determine the cipher length
		if (encryption != null) {
			cipherlen = encryption.getBlockSize();
		}

		// Compress the payload if necersary
		if (outgoingCompression != null) {
			payload = outgoingCompression.compress(payload, 0,
					payload.length);
		}

		// Determine the padding length
		padding += ((cipherlen - ((payload.length + 1 + padding) % cipherlen)) % cipherlen);

		// Write the packet length field
		outgoingMessage.putInt(payload.length + 1 + padding);

		// Write the padding length
		outgoingMessage.put((byte) padding);

		// Write the message payload
		outgoingMessage.put(payload, 0, payload.length);
		outgoingBytes += payload.length + padding + 1;

		// Create some random data for the padding
		byte[] pad = new byte[padding];
		rnd.nextBytes(pad);

		// Write the padding
		outgoingMessage.put(pad);

		outgoingMessage.flip();
		
		// Get the unencrypted packet data
		byte[] packet;
		if(encryption!=null && encryption.isMAC()) {
			packet = new byte[outgoingMessage.remaining() + encryption.getMacLength()];
		} else {
			packet = new byte[outgoingMessage.remaining()];
		}
		
		outgoingMessage.get(packet);
		byte[] mac = null;

		// Perfrom encrpytion
		if (encryption != null) {
			encryption.transform(packet, 4, packet, 4, packet.length-4);
		}
		
		// Generate the MAC
		if (outgoingMac != null) {
			mac = new byte[outgoingMac.getMacLength()];
			outgoingMac.generate(outgoingSequence, packet, 0,
					packet.length, mac, 0);
		}

		// Reset the message
		outgoingMessage.clear();

		// Write the packet data
		outgoingMessage.put(packet);

		// Combine the packet and MAC
		if (mac != null && mac.length > 0) {
			outgoingMessage.put(mac);
			outgoingBytes += mac.length;
		}
		
	}

	private void encodeOriginalFormatPacket(ByteBuffer outgoingMessage) throws IOException {
		
		/**
		 * Wrap the message payload into the binary packet
		 * format
		 */
		byte[] payload = new byte[outgoingMessage.remaining()];
		outgoingMessage.get(payload);
		outgoingMessage.clear();

		if(Log.isTraceEnabled()) {
			Log.raw(Level.TRACE, Utils.bytesToHex(payload, 0, payload.length, 32, true, true), true);
		}
		
		int padding = 4;
		int cipherlen = 8;

		// Determine the cipher length
		if (encryption != null) {
			cipherlen = encryption.getBlockSize();
		}

		// Compress the payload if necersary
		if (outgoingCompression != null) {
			payload = outgoingCompression.compress(payload, 0,
					payload.length);
		}

		// Determine the padding length
		if(encryption!=null && encryption.isMAC()) {
			padding += ((cipherlen - ((payload.length + 1 + padding) % cipherlen)) % cipherlen);
		} else {
			padding += ((cipherlen - ((payload.length + 5 + padding) % cipherlen)) % cipherlen);
		}
		
		// Write the packet length field
		int msglen = payload.length + 1 + padding;
		outgoingMessage.putInt(msglen);

		// Write the padding length
		outgoingMessage.put((byte) padding);

		// Write the message payload
		outgoingMessage.put(payload, 0, payload.length);
		outgoingBytes += payload.length + padding + 5;

		// Create some random data for the padding
		byte[] pad = new byte[padding];
		rnd.nextBytes(pad);

		// Write the padding
		outgoingMessage.put(pad);

		outgoingMessage.flip();
		// Get the unencrypted packet data
		byte[] packet;
		
		if(encryption!=null && encryption.isMAC()) {
			packet = new byte[outgoingMessage.remaining() + encryption.getMacLength()];
		} else {
			packet = new byte[outgoingMessage.remaining()];
		}
		outgoingMessage.get(packet, 0, outgoingMessage.remaining());
		byte[] mac = null;

		// Generate the MAC
		if (outgoingMac != null) {
			mac = new byte[outgoingMac.getMacLength()];
			outgoingMac.generate(outgoingSequence, packet, 0,
					packet.length, mac, 0);
		}

		// Perfrom encrpytion
		if (encryption != null) {
			if(encryption.isMAC()) {
				encryption.transform(packet, 0, packet, 0, msglen+4);
			} else {
				encryption.transform(packet);
			}
		}

		// Reset the message
		outgoingMessage.clear();

		// Write the packet data
		outgoingMessage.put(packet);

		// Combine the packet and MAC
		if (mac != null && mac.length > 0) {
			outgoingMessage.put(mac);
			outgoingBytes += mac.length;
		}
		
	}

	public int getState() {
		return currentState;
	}

	/**
	 * Returns the local address to which the remote socket is connected.
	 * 
	 * @return InetAddress
	 */
	public SocketAddress getLocalAddress() {
		return socketConnection.getLocalAddress();
	}

	/**
	 * Returns the local port to which the remote socket is connected.
	 * 
	 * @return int
	 */
	public int getLocalPort() {
		return socketConnection.getLocalPort();
	}

	public String getRemoteIdentification() {
		return remoteIdentification.toString();
	}

	public String getUUID() {
		return uuid.toString();
	}

	protected abstract SocketAddress getConnectionAddress();
	
	/**
	 * Disconnect from the remote host. No more messages can be sent after this
	 * method has been called.
	 * 
	 * @param reason
	 * @param description
	 * @throws IOException
	 */
	public void disconnect(int reason, String description) {
		if (description == null)
			description = "Failure";
		disconnectStarted = new Date();
		if(Log.isInfoEnabled()) {
			Log.info("Disconnect {} {}", 
					getConnectionAddress().toString(),
					description);
		}
		postMessage(new DisconnectMessage(reason, description));
	}

	/**
	 * Disconnects everything internally
	 */
	public void onSocketClose() {

		synchronized (this) {
			if (!closed) {
				Connection<T> connection = getConnection();

				closed = true;
	
				if(Log.isInfoEnabled()) {
					Log.info("Connection closed {}", 
							getConnectionAddress().toString());
				}
				
				if (disconnectStarted == null)
					disconnectStarted = new Date();

				if(Log.isDebugEnabled())
					Log.debug("Performing internal disconnect {}", getUUID());
				
				setTransportState(TransportProtocol.DISCONNECTED);

				if (socketConnection != null)
					socketConnection.getIdleStates().remove(TransportProtocol.this);

				if (activeService != null) {
					if(Log.isDebugEnabled())
						Log.debug("Stopping the active service");
					activeService.stop();
				}

				if(Log.isDebugEnabled())
					Log.debug("Logging off user");

				for (Iterator<TransportProtocolListener> it = transportListeners
						.iterator(); it.hasNext();) {
					it.next().onDisconnect(TransportProtocol.this);
				}
				
				if(Log.isDebugEnabled()) {
					Log.debug("Submitting transport cleanup to executor service");
				}

				
				if(connection != null) {
					/* Connection may be null if a socket connection was made by the protocol never started */
					addTask(EVENTS, new ConnectionTaskWrapper(connection, new Runnable() {
						public void run() {
							synchronized (lock) {
								cleanupOperations(new ConnectionAwareTask(con) {
									protected void doTask() {
										
										disconnected();
										onDisconnected();
										disconnectFuture.disconnected();
										
										EventServiceImplementation
										.getInstance()
										.fireEvent(
												new Event(
														this,
														EventCodes.EVENT_DISCONNECTED,
														true)
														.addAttribute(
																EventCodes.ATTRIBUTE_CONNECTION,
																con)
														.addAttribute(
																EventCodes.ATTRIBUTE_OPERATION_STARTED,
																disconnectStarted)
														.addAttribute(
																EventCodes.ATTRIBUTE_OPERATION_FINISHED,
																new Date()));
										
	
									}
								});
							}
						}
					}));
				}
				
				
			}
		}

	}

	/**
	 * Gets the secure random number generator for this transport.
	 * 
	 * @return the secure RND
	 */
	public SecureRandom getRND() {
		return rnd;
	}

	void setTransportState(int transportState) {
		currentState = transportState;
	}

	protected abstract void initializeKeyExchange(SshKeyExchange<T> keyExchange, 
			boolean firstPacketFollows, 
			boolean useFirstPacket) throws IOException, SshException;

	int incomingCipherLength = 8;
	int incomingMacLength = 0;
	int msglen = 0;
	int padlen = 0;
	int remaining = 0;
	byte[] initial;

	/**
	 * Perform key exchange
	 * 
	 * @param msg
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	void performKeyExchange(byte[] msg) throws IOException,
			WriteOperationRequest {

		ByteArrayReader bar = null;

		if(!postedIdentification) {
			sendLocalIdentification();
		}
		
		checkAlgorithms();
		
		try {

			// Set the state to performing key exchange now that we have both
			// msgs
			currentState = TransportProtocol.PERFORMING_KEYEXCHANGE;

			// Extract the remote's side kex init taking away the header and
			// padding
			remotekex = msg;

			bar = new ByteArrayReader(remotekex, 0, remotekex.length);
			bar.skip(17);

			remoteKeyExchanges = checkValidString("key exchange",
					bar.readString());
			remotePublicKeys = checkValidString("public key",
					bar.readString());
			remoteCiphersCS = checkValidString("client->server cipher",
					bar.readString());
			remoteCiphersSC = checkValidString("server->client cipher",
					bar.readString());
			remoteCSMacs = bar.readString();
			remoteSCMacs = bar.readString();
			remoteCSCompressions = bar.readString();
			remoteSCCompressions = bar.readString();

			// Read language strings and ignore as don't support other languages
			String lang = bar.readString();
			lang = bar.readString();

			boolean firstPacketFollows = (bar.read() != 0);
			
			onKeyExchangeInit();
			
			// Send our kex init (this will only be sent if needed to)
			sendKeyExchangeInit();

			// Determine the negotiated key exchange
			String localKeyExchanges = sshContext.supportedKeyExchanges().list(
								sshContext.getPreferredKeyExchange());
			
			String localCiphersCS = sshContext
					.supportedCiphersCS()
					.list(sshContext
							.getPreferredCipherCS());
			
			String localCiphersSC = sshContext
					.supportedCiphersSC()
					.list(sshContext
							.getPreferredCipherSC());
			
			String localPublicKeys = sshContext.getPublicKeys();
			
			String localMacsCS = sshContext
					.supportedMacsCS()
					.list(sshContext
							.getPreferredMacCS());
			
			
			String localMacsSC = sshContext
					.supportedMacsSC()
					.list(sshContext
							.getPreferredMacSC());
			
			String localCompressionCS = sshContext
					.supportedCompressionsCS()
					.list(sshContext
							.getPreferredCompressionCS());
			
			String localCompressionSC = sshContext
					.supportedCompressionsSC()
					.list(sshContext
							.getPreferredCompressionSC());
			
			if(Log.isDebugEnabled()) {
				Log.debug("Remote Key Exchanges: {}", remoteKeyExchanges);
				Log.debug("Remote Public Keys: {}", remotePublicKeys);
				Log.debug("Remote Ciphers CS: {}", remoteCiphersCS);
				Log.debug("Remote Ciphers SC: {}", remoteCiphersSC);
				Log.debug("Remote Macs CS: {}", remoteCSMacs);
				Log.debug("Remote Macs SC: {}", remoteSCMacs);
				Log.debug("Remote Compression CS: {}", remoteCSCompressions);
				Log.debug("Remote Compression SC: {}", remoteSCCompressions);
				Log.debug("Lang: {}", lang);
				Log.debug("First Packet Follows: {}", firstPacketFollows);
				Log.debug("Local Key Exchanges: {}", localKeyExchanges);
				Log.debug("Local Public Keys: {}", localPublicKeys);
				Log.debug("Local Ciphers CS: {}", localCiphersCS);
				Log.debug("Local Ciphers SC: {}", localCiphersSC);
				Log.debug("Local Macs CS: {}", localMacsCS);
				Log.debug("Local Macs SC: {}", localMacsSC);
				Log.debug("Local Compression CS: {}", localCompressionCS);
				Log.debug("Local Compression SC: {}", localCompressionSC);
			}
			

			keyExchangeAlgorithm = selectNegotiatedComponent(
					remoteKeyExchanges, localKeyExchanges);
			keyExchange = (SshKeyExchange<T>) sshContext
					.supportedKeyExchanges().getInstance(keyExchangeAlgorithm);
			// Get the server's public key for the preferred algorithm

			publicKey = selectNegotiatedComponent(remotePublicKeys,
					sshContext.getSupportedPublicKeys());

			// Determine if we should use the first packet
			boolean useFirstPacket = remoteKeyExchanges.startsWith(sshContext
					.getPreferredKeyExchange())
					&& remotePublicKeys.startsWith(sshContext
							.getPreferredPublicKey());

			EventServiceImplementation
					.getInstance()
					.fireEvent(
							(new Event(this,
									EventCodes.EVENT_KEY_EXCHANGE_INIT,
									true))
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											con)
									.addAttribute(
											EventCodes.ATTRIBUTE_REMOTE_KEY_EXCHANGES,
											remoteKeyExchanges)
									.addAttribute(
											EventCodes.ATTRIBUTE_LOCAL_KEY_EXCHANGES,
											localKeyExchanges)
									.addAttribute(
											EventCodes.ATTRIBUTE_REMOTE_PUBLICKEYS,
											remotePublicKeys)
									.addAttribute(
											EventCodes.ATTRIBUTE_LOCAL_PUBLICKEYS,
											localPublicKeys)
									.addAttribute(
											EventCodes.ATTRIBUTE_REMOTE_CIPHERS_CS,
											remoteCiphersCS)
									.addAttribute(
											EventCodes.ATTRIBUTE_LOCAL_CIPHERS_CS,
											localCiphersCS)
									.addAttribute(
											EventCodes.ATTRIBUTE_REMOTE_CIPHERS_SC,
											remoteCiphersSC)
									.addAttribute(
											EventCodes.ATTRIBUTE_LOCAL_CIPHERS_SC,
											localCiphersSC)
									.addAttribute(
											EventCodes.ATTRIBUTE_REMOTE_CS_MACS,
											remoteCSMacs)
									.addAttribute(
											EventCodes.ATTRIBUTE_LOCAL_CS_MACS,
											localMacsCS)
									.addAttribute(
											EventCodes.ATTRIBUTE_REMOTE_SC_MACS,
											remoteSCMacs)
									.addAttribute(
											EventCodes.ATTRIBUTE_LOCAL_SC_MACS,
											localMacsSC)
									.addAttribute(
											EventCodes.ATTRIBUTE_REMOTE_CS_COMPRESSIONS,
											remoteCSCompressions)
									.addAttribute(
											EventCodes.ATTRIBUTE_LOCAL_CS_COMPRESSIONS,
											localCompressionCS)
									.addAttribute(
											EventCodes.ATTRIBUTE_REMOTE_SC_COMPRESSIONS,
											remoteSCCompressions)
									.addAttribute(
											EventCodes.ATTRIBUTE_LOCAL_SC_COMPRESSIONS,
											localCompressionSC));
			// Initialize the key exchange
			initializeKeyExchange(keyExchange, firstPacketFollows, useFirstPacket);

			cipherCS = selectNegotiatedComponent(
					checkValidString("client->server cipher list",
							remoteCiphersCS), localCiphersCS);

			cipherSC = selectNegotiatedComponent(
					checkValidString("server->client cipher list",
							remoteCiphersSC), localCiphersSC);

			macCS = selectNegotiatedComponent(
					checkValidString("client->server hmac list", remoteCSMacs),
					localMacsCS);

			macSC = selectNegotiatedComponent(
					checkValidString("server->client hmac list", remoteSCMacs),
					localMacsSC);

			compressionCS = selectNegotiatedComponent(
					checkValidString("client->server compression list",
							remoteCSCompressions),
					localCompressionCS);

			compressionSC = selectNegotiatedComponent(
					checkValidString("server->client compression list",
							remoteSCCompressions),
					localCompressionSC);

			if(Log.isDebugEnabled()) {
				Log.debug("Negotiated Key Exchange: {}", keyExchangeAlgorithm);
				Log.debug("Negotiated Public Key: {}", publicKey);
				Log.debug("Negotiated Cipher CS: {}", cipherCS);
				Log.debug("Negotiated Cipher SC: {}", cipherSC);
				Log.debug("Negotiated Mac CS: {}", macCS);
				Log.debug("Negotiated Mac SC: {}", macSC);
				Log.debug("Negotiated Compression CS: {}", compressionCS);
				Log.debug("Negotiated Compression SC: {}", compressionSC);
			}
			
			keyExchangeInitialized();

		} catch (SshException ex) {
			if(ex.getCause()!=null) {
				ex.getCause().printStackTrace();
			}
			EventServiceImplementation.getInstance().fireEvent(
					new Event(this,
							EventCodes.EVENT_KEY_EXCHANGE_FAILURE, true)
							.addAttribute(EventCodes.ATTRIBUTE_CONNECTION,
									con));
			throw new IOException("Unexpected protocol termination: "
					+ ex.getMessage());
		} finally {
			if (bar != null) {
				bar.close();
			}
		}
	}

	protected abstract void onKeyExchangeInit() throws SshException;

	private void checkAlgorithms() {
		if(Boolean.getBoolean("maverick.isolate")) {
			String kex = System.getProperty("maverick.isolatedKex", SshContext.KEX_DIFFIE_HELLMAN_ECDH_NISTP_256);
			String cipher = System.getProperty("maverick.isolatedCipher", SshContext.CIPHER_AES128_CTR);
			String mac = System.getProperty("maverick.isolatedMac", SshContext.HMAC_SHA1);
			String compression = System.getProperty("maverick.isolatedComp", SshContext.COMPRESSION_NONE);
			String pk = System.getProperty("maverick.isolatedPublicKey", SshContext.PUBLIC_KEY_SSHRSA);
			
			getContext().supportedKeyExchanges().removeAllBut(kex);
			getContext().supportedCiphersCS().removeAllBut(cipher);
			getContext().supportedCiphersSC().removeAllBut(cipher);
			getContext().supportedMacsCS().removeAllBut(mac);
			getContext().supportedMacsSC().removeAllBut(mac);
			getContext().supportedCompressionsCS().removeAllBut(compression);
			getContext().supportedCompressionsSC().removeAllBut(compression);
			getContext().supportedPublicKeys().removeAllBut(pk);
		}
	}
	protected void keyExchangeInitialized() {

	}

	protected abstract void disconnected();
	
	protected abstract void onNewKeysReceived();
	
	protected abstract boolean processTransportMessage(int msgid, byte[] msg) throws IOException, SshException;
	
	
	/**
	 * Process a message. This should be called when reading messages from
	 * outside of the transport protocol so that the transport protocol can
	 * parse its own messages.
	 * 
	 * @param msg
	 * @return <code>true</code> if the message was processed by the transport
	 *         and can be discarded, otherwise <code>false</code>.
	 * @throws IOException
	 */
	public void processMessage(byte[] msg, long sequenceNo) throws SshException,
			IOException, WriteOperationRequest {

		resetIdleState(this);

		if (msg.length < 1) {
			throw new IOException("Invalid transport protocol message");
		}

		if(Log.isTraceEnabled()) {
			Log.raw(Level.TRACE, Utils.bytesToHex(msg, 32, true, true), true);
		}
		
		int msgId = msg[0];
		
		if(Log.isTraceEnabled()) {
			Log.debug("Processing transport protocol message id {}", msgId);
		}
		
		switch (msgId) {
		case SSH_MSG_DISCONNECT: {

			ByteArrayReader bar = new ByteArrayReader(msg);
			try {
				bar.skip(5);
				if(Log.isDebugEnabled()) {
					Log.debug("Recieved SSH_MSG_DISCONNECT {}", bar.readString());
				}
				socketConnection.closeConnection();
			} finally {
				bar.close();
			}
			break;
		}
		case SSH_MSG_IGNORE:

			if(Log.isDebugEnabled())
				Log.debug("Received SSH_MSG_IGNORE");
			break;
		case SSH_MSG_DEBUG:

			if(Log.isDebugEnabled())
				Log.debug("Received SSH_MSG_DEBUG");
			break;
		case SSH_MSG_NEWKEYS:

			if(Log.isDebugEnabled())
				Log.debug("Received SSH_MSG_NEWKEYS");
			synchronized (keyExchange) {
				keyExchange.setReceivedNewKeys(true);
				// Put the keys into use
				onNewKeysReceived();
			}

			break;
		case SSH_MSG_KEX_INIT: {
			if(Log.isDebugEnabled())
				Log.debug("Received SSH_MSG_KEX_INIT");
			performKeyExchange(msg);
			break;
		}
		case SSH_MSG_UNIMPLEMENTED: {
			ByteArrayReader bar = new ByteArrayReader(msg);
			try {
				bar.skip(1);
				if(Log.isDebugEnabled())
					Log.debug("Received SSH_MSG_UNIMPLEMENTED for sequence {}", bar.readInt());
			} finally {
				bar.close();
			}
			if(Boolean.getBoolean("maverick.failOnUnimplemented")) {
				throw new IllegalStateException("SSH_MSG_UNIMPLEMENTED message returned by remote");
			}
			break;
		}
		default: {

			if(processTransportMessage(msgId, msg)) {
				return;
			}
			// Not a transport protocol message so try key exchange
			if (currentState == TransportProtocol.PERFORMING_KEYEXCHANGE) {
				if (keyExchange.processMessage(msg)) {
					break;
				}
			}
			
			if(Log.isTraceEnabled()) {
				Log.trace("Posting mesage id {} to active service for processing", msgId);
			}
			
			addTask(ACTIVE_SERVICE_IN, new ConnectionAwareTask(con) {
				protected void doTask() {
					try {
						
						if(Log.isTraceEnabled()) {
							Log.trace("Processing active service message id {}", msgId);
						}
						
						// Not a key exchange message so try the active service
						if (activeService != null && activeService.processMessage(msg)) {
							return;
						}

						/**
						 * If we reached here we have an unimplemented message
						 */
						if(Log.isDebugEnabled()) {
							Log.debug("Unimplemented Message id={}", msg[0]);
						}
						postMessage(new UnimplementedMessage(sequenceNo));
					} catch (IOException | SshException e) {
						disconnect(PROTOCOL_ERROR, e.getMessage());
					}
				}
			});
		   }
		}
	}

	protected abstract void onNewKeysSent();
	
	public void sendNewKeys() {
		// We can now put the new keys into use
		postMessage(new SshMessage() {
			public boolean writeMessageIntoBuffer(ByteBuffer buf) {
				buf.put((byte) TransportProtocol.SSH_MSG_NEWKEYS);
				return true;
			}

			public void messageSent(Long sequenceNo) {
				// Potentially generate keys????
				synchronized (keyExchange) {
					if(Log.isDebugEnabled())
						Log.debug("Sent SSH_MSG_NEWKEYS");
					keyExchange.setSentNewKeys(true);
					onNewKeysSent();
				}
			}
		}, true);

	}

	public T getSshContext() {
		return sshContext;
	}

	protected String selectNegotiatedComponent(String clientlist, String serverlist)
			throws IOException {

		Vector<String> r = new Vector<String>();
		int idx;
		String name;
		while ((idx = serverlist.indexOf(",")) > -1) {
			r.addElement(serverlist.substring(0, idx).trim());
			serverlist = serverlist.substring(idx + 1).trim();
		}

		r.addElement(serverlist);

		while ((idx = clientlist.indexOf(",")) > -1) {
			name = clientlist.substring(0, idx).trim();
			if (r.contains(name)) {
				return name;
			}
			clientlist = clientlist.substring(idx + 1).trim();
		}

		if (r.contains(clientlist)) {
			return clientlist;
		}
		EventServiceImplementation
				.getInstance()
				.fireEvent(
						(new Event(
								this,
								EventCodes.EVENT_FAILED_TO_NEGOTIATE_TRANSPORT_COMPONENT,
								true))
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										con)
								.addAttribute(
										EventCodes.ATTRIBUTE_LOCAL_COMPONENT_LIST,
										serverlist)
								.addAttribute(
										EventCodes.ATTRIBUTE_REMOTE_COMPONENT_LIST,
										clientlist));
		throw new IOException(String.format("Failed to negotiate a transport component from {} and {}", clientlist, serverlist));

	}

	protected void completeKeyExchange(SshKeyExchange<T> keyExchange) {
		
		localkex = null; 
		remotekex = null;
		completedFirstKeyExchange = true;
		
		EventServiceImplementation.getInstance()
			.fireEvent(
				(new Event(this,
						EventCodes.EVENT_KEY_EXCHANGE_COMPLETE,
						true))
						.addAttribute(
								EventCodes.ATTRIBUTE_CONNECTION,
								con)
						.addAttribute(
								EventCodes.ATTRIBUTE_USING_PUBLICKEY,
								publicKey)
						.addAttribute(
								EventCodes.ATTRIBUTE_USING_KEY_EXCHANGE,
								keyExchangeAlgorithm)
						.addAttribute(
								EventCodes.ATTRIBUTE_USING_CS_CIPHER,
								cipherCS)
						.addAttribute(
								EventCodes.ATTRIBUTE_USING_SC_CIPHER,
								cipherSC)
						.addAttribute(
								EventCodes.ATTRIBUTE_USING_CS_MAC,
								macCS)
						.addAttribute(
								EventCodes.ATTRIBUTE_USING_SC_MAC,
								macSC)
						.addAttribute(
								EventCodes.ATTRIBUTE_USING_CS_COMPRESSION,
								compressionCS)
						.addAttribute(
								EventCodes.ATTRIBUTE_USING_SC_COMPRESSION,
								compressionSC));
		
		setTransportState(TransportProtocol.CONNECTED);

	}
	
	protected void generateNewKeysServerOut() {

		synchronized (kexlockOut) {
			try {
				// The first exchange hash is the session identifier
				if (sessionIdentifier == null) {
					sessionIdentifier = keyExchange.getExchangeHash();
				}

				// Generate a new set of context components
				encryption = (SshCipher) sshContext.supportedCiphersSC()
						.getInstance(cipherSC);
				// Create the new keys and initialize all the components
				encryption.init(SshCipher.ENCRYPT_MODE, makeSshKey('B', encryption.getBlockSize()),
						makeSshKey('D', encryption.getKeyLength()));
				
				if(!encryption.isMAC()) {
					outgoingMac = (SshHmac) sshContext.supportedMacsSC()
						.getInstance(macSC);
					outgoingMac.init(makeSshKey('F', outgoingMac.getMacSize()));
				}
				
				outgoingCompression = null;

				if (!compressionSC.equals(SshContext.COMPRESSION_NONE)) {
					outgoingCompression = (SshCompression) sshContext
							.supportedCompressionsSC().getInstance(
									compressionSC);
					outgoingCompression.init(SshCompression.DEFLATER,
							getSshContext().getCompressionLevel());
				}

				if (keyExchange.hasReceivedNewKeys()) {
					completeKeyExchange(keyExchange);
				}

			} catch (Throwable ex) {
				if(Log.isDebugEnabled())
					Log.debug("Failed to create transport component", ex);
				connectFuture.done(false);
				if(disconnectStarted != null)
					disconnect(
						TransportProtocol.PROTOCOL_ERROR,
						"Failed to create a transport component! "
								+ ex.getMessage());
			}
		}
	}

	protected void generateNewKeysServerIn() {
		synchronized (kexlockIn) {
			try {
				// The first exchange hash is the session identifier
				if (sessionIdentifier == null) {
					sessionIdentifier = keyExchange.getExchangeHash();

				}

				// Generate a new set of context components
				decryption = (SshCipher) sshContext.supportedCiphersCS()
						.getInstance(cipherCS);
				// Put the incoming components into use
				decryption.init(SshCipher.DECRYPT_MODE, makeSshKey('A', decryption.getBlockSize()),
						makeSshKey('C', decryption.getKeyLength()));
				
				if(!decryption.isMAC()) {
					incomingMac = (SshHmac) sshContext.supportedMacsCS()
						.getInstance(macCS);
					incomingMac.init(makeSshKey('E', incomingMac.getMacSize()));
					incomingMacLength = incomingMac.getMacLength();
				} else {
					incomingMacLength = decryption.getMacLength();
				}
				
				incomingCompression = null;

				if (!compressionCS.equals(SshContext.COMPRESSION_NONE)) {
					incomingCompression = (SshCompression) sshContext
							.supportedCompressionsCS().getInstance(
									compressionCS);
					incomingCompression.init(SshCompression.INFLATER,
							getSshContext().getCompressionLevel());
				}

				incomingCipherLength = decryption.getBlockSize();

				if (keyExchange.hasSentNewKeys()) {
					completeKeyExchange(keyExchange);
				}

			} catch (Throwable ex) {
				if(Log.isDebugEnabled())
					Log.debug("Failed to create transport component", ex);
				connectFuture.done(false);
				if(disconnectStarted != null)
					disconnect(
						TransportProtocol.PROTOCOL_ERROR,
						"Failed to create a transport component! "
								+ ex.getMessage());
			}
		}
	}

	
	protected void generateNewKeysClientOut() {

		synchronized (kexlockOut) {
			try {
				// The first exchange hash is the session identifier
				if (sessionIdentifier == null) {
					sessionIdentifier = keyExchange.getExchangeHash();
				}

				// Generate a new set of context components
				encryption = (SshCipher) sshContext.supportedCiphersSC()
						.getInstance(cipherCS);
				// Create the new keys and initialize all the components
				encryption.init(SshCipher.ENCRYPT_MODE, makeSshKey('A', encryption.getBlockSize()),
						makeSshKey('C', encryption.getKeyLength()));
				
				if(!encryption.isMAC()) {
					outgoingMac = (SshHmac) sshContext.supportedMacsSC()
						.getInstance(macCS);
					outgoingMac.init(makeSshKey('E', outgoingMac.getMacSize()));
				}
				
				outgoingCompression = null;

				if (!compressionSC.equals(SshContext.COMPRESSION_NONE)) {
					outgoingCompression = (SshCompression) sshContext
							.supportedCompressionsSC().getInstance(
									compressionCS);
					outgoingCompression.init(SshCompression.DEFLATER,
							getSshContext().getCompressionLevel());
				}

				if (keyExchange.hasReceivedNewKeys()) {
					completeKeyExchange(keyExchange);
				}

			} catch (Throwable ex) {
				if(Log.isErrorEnabled())
					Log.error("Failed to create transport component", ex);
				connectFuture.done(false);
				if(disconnectStarted != null)
					disconnect(
						TransportProtocol.PROTOCOL_ERROR,
						"Failed to create a transport component! "
								+ ex.getMessage());
			}
		}
	}

	protected void generateNewKeysClientIn() {
		
		synchronized (kexlockIn) {
			try {
				// The first exchange hash is the session identifier
				if (sessionIdentifier == null) {
					sessionIdentifier = keyExchange.getExchangeHash();

				}

				// Generate a new set of context components
				decryption = (SshCipher) sshContext.supportedCiphersCS()
						.getInstance(cipherSC);
				// Put the incoming components into use
				decryption.init(SshCipher.DECRYPT_MODE, makeSshKey('B', decryption.getBlockSize()),
						makeSshKey('D', decryption.getKeyLength()));
				
				if(!decryption.isMAC()) {
					incomingMac = (SshHmac) sshContext.supportedMacsCS()
						.getInstance(macSC);
					incomingMac.init(makeSshKey('F', incomingMac.getMacSize()));
					incomingMacLength = incomingMac.getMacLength();
				} else {
					incomingMacLength = decryption.getMacLength();
				}
				
				incomingCompression = null;

				if (!compressionCS.equals(SshContext.COMPRESSION_NONE)) {
					incomingCompression = (SshCompression) sshContext
							.supportedCompressionsCS().getInstance(
									compressionSC);
					incomingCompression.init(SshCompression.INFLATER,
							getSshContext().getCompressionLevel());
				}

				incomingCipherLength = decryption.getBlockSize();

				if (keyExchange.hasSentNewKeys()) {
					completeKeyExchange(keyExchange);
				}

			} catch (Throwable ex) {
				if(Log.isErrorEnabled())
					Log.error("Failed to create transport component", ex);
				connectFuture.done(false);
				if(disconnectStarted != null)
					disconnect(
						TransportProtocol.PROTOCOL_ERROR,
						"Failed to create a transport component! "
								+ ex.getMessage());
			}
		}
	}
	
	void sendKeyExchangeInit() {

		try {

			synchronized (kexlockOut) {

				numIncomingBytesSinceKEX = 0;
				numIncomingPacketsSinceKEX = 0;
				numOutgoingBytesSinceKEX = 0;
				numOutgoingPacketsSinceKEX = 0;

				setTransportState(PERFORMING_KEYEXCHANGE);

				if (localkex == null) {

					try {
						localkex = TransportProtocolHelper.generateKexInit(getContext());

						kexQueue.clear();
						if(Log.isDebugEnabled())
							Log.debug("Posting SSH_MSG_KEX_INIT");
						postMessage(new SshMessage() {
							public boolean writeMessageIntoBuffer(ByteBuffer buf) {
								buf.put(localkex);
								return true;
							}

							public void messageSent(Long sequenceNo) {
								if(Log.isDebugEnabled())
									Log.debug("Sent SSH_MSG_KEX_INIT");
							}
						}, true);

					} catch(SshException e) { 
						disconnect(BY_APPLICATION, "Internal error");
					}

				}
			}
		} catch (IOException ex) {
			disconnect(TransportProtocol.PROTOCOL_ERROR,
					"Failed to create SSH_MSG_KEX_INIT");
		}

	}

 	public String getCipherCS() {
 		return cipherCS;
	}
	
	public String getCipherSC() {
		return cipherSC;
	}
	
	public String getMacCS() {
		return macCS;
	}
	
	public String getMacSC() {
		return macSC;
	}
	
	public String getCompressionCS() {
		return compressionCS;
	}
	
	public String getCompressionSC() {
		return compressionSC;
	}
	
	private String checkValidString(String id, String str) throws IOException {

		if (str.trim().equals(""))
			throw new IOException("Client sent invalid " + id + " value '"
					+ str + "'");

		StringTokenizer t = new StringTokenizer(str, ",");

		if (!t.hasMoreElements())
			throw new IOException("Client sent invalid " + id + " value '"
					+ str + "'");
		return str;
	}

	@Override
	public void postMessage(SshMessage msg) {
		if (!msg.equals(ignoreMessage) && !(msg instanceof TransportProtocol.DisconnectMessage))
			resetIdleState(this);
		postMessage(msg, false);
	}
	
	@Override
	public void postMessage(SshMessage msg, boolean kex) {
		
		if(Log.isTraceEnabled())
			Log.debug("Posting message " + msg.getClass().getName()
					+ " to queue");

		LinkedList<SshMessage> list = kex && completedFirstKeyExchange ? kexQueue
				: outgoingQueue;
		synchronized (kexlockOut) {
			list.addLast(msg);
		}
		
		socketConnection.flagWrite();
	}

	byte[] makeSshKey(char chr, int sizeRequired) throws SshException, IOException {

		// Create the first 20 bytes of key data
		ByteArrayWriter keydata = new ByteArrayWriter();

		try {
			byte[] data = new byte[20];

			Digest hash = (Digest) ComponentManager.getDefaultInstance()
					.supportedDigests()
					.getInstance(keyExchange.getHashAlgorithm());

			// Put the dh k value
			hash.putBigInteger(keyExchange.getSecret());

			// Put in the exchange hash
			hash.putBytes(keyExchange.getExchangeHash());

			// Put in the character
			hash.putByte((byte) chr);

			// Put in the session identifier
			hash.putBytes(sessionIdentifier);

			// Create the first 20 bytes
			data = hash.doFinal();

			keydata.write(data);

			while(keydata.size() < sizeRequired) {
				// Now do the next 20
				hash.reset();
	
				// Put the dh k value in again
				hash.putBigInteger(keyExchange.getSecret());
	
				// And the exchange hash
				hash.putBytes(keyExchange.getExchangeHash());
	
				// Finally the first 20 bytes of data we created
				hash.putBytes(data);
	
				data = hash.doFinal();
	
				// Put it all together
				keydata.write(data);
			}

			// Return it
			return keydata.toByteArray();

		} finally {
			keydata.close();
		}

	}

	class IgnoreMessage implements SshMessage {

		SecureRandom rnd = new SecureRandom();
		byte[] tmp = new byte[getContext().getKeepAliveDataMaxLength()];

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put((byte) SSH_MSG_IGNORE);
			int len = (int) (Math.random() * (tmp.length + 1));
			rnd.nextBytes(tmp);
			buf.putInt(len);
			buf.put(tmp, 0, len);
			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled())
				Log.debug("Sent SSH_MSG_IGNORE");
		}

	}

	class UnimplementedMessage implements SshMessage {

		long sequenceNo;

		UnimplementedMessage(long sequenceNo) {
			this.sequenceNo = sequenceNo;
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put((byte) SSH_MSG_UNIMPLEMENTED);
			buf.putInt((int) sequenceNo);
			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled())
				Log.debug("Sent SSH_MSG_UNIMPLEMENTED");
		}
	}

	class DisconnectMessage implements SshMessage {

		int reason;
		String description;

		DisconnectMessage(int reason, String description) {
			this.reason = reason;
			this.description = description;
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put((byte) SSH_MSG_DISCONNECT);
			buf.putInt(reason);
			buf.putInt(description.length());
			buf.put(description.getBytes());
			buf.putInt(0);
			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled())
				Log.debug("Sent SSH_MSG_DISCONNECT reason=" + reason + " "
						+ description);
			socketConnection.closeConnection();
		}
	}

	public byte[] getSessionKey() {
		return sessionIdentifier;
	}
	
	public static byte[] getBytes(String str, String charset) {
		try {
			return str.getBytes(charset);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("System does not support " + charset);
		}
	}

	public void kill() {
		socketConnection.closeConnection();
	}

	public String getHostKeyAlgorithm() {
		return publicKey;
	}
	
	public SshPublicKey getHostKey() {
		return hostKey;
	}
	
	public String getKeyExchangeAlgorithm() {
		return keyExchangeAlgorithm;
	}
	
	public String[] getRemoteKeyExchanges() {
		return remoteKeyExchanges.split(",");
	}

	public String[] getRemotePublicKeys() {
		return remotePublicKeys.split(",");
	}

	public String[] getRemoteCiphersCS() {
		return remoteCiphersCS.split(",");
	}

	public String[] getRemoteCiphersSC() {
		return remoteCiphersSC.split(",");
	}

	public String[] getRemoteMacsCS() {
		return remoteCSMacs.split(",");
	}

	public String[] getRemoteMacsSC() {
		return remoteSCMacs.split(",");
	}

	public String[] getRemoteCompressionsCS() {
		return remoteCSCompressions.split(",");
	}

	public String[] getRemoteCompressionsSC() {
		return remoteSCCompressions.split(",");
	}
	
	public boolean hasCompletedKeyExchange() {
		return completedFirstKeyExchange;
	}
	
	public ExecutorOperationSupport<?> getExecutor() {
		return this;
	}

	public void registerIdleStateListener(IdleStateListener listener) {
		idleListeners.add(listener);
	}

	public void removeIdleStateListener(IdleStateListener listener) {
		idleListeners.remove(listener);
	}

	public void resetIdleState(IdleStateListener listener) {
		
		lastActivity = System.currentTimeMillis();
		if (getContext().getIdleConnectionTimeoutSeconds() > 0
				&& socketConnection != null)
			socketConnection.getIdleStates().reset(this);
	}

	public boolean isSelectorThread() {
		return Thread.currentThread().equals(getSocketConnection().getSelectorThread());
	}

	public String getKeyExchangeInUse() {
		return keyExchangeAlgorithm;
	}

	public String getHostKeyInUse() {
		return publicKey;
	}

	public String getLocalIdentification() {
		return localIdentification.trim();
	}
}
