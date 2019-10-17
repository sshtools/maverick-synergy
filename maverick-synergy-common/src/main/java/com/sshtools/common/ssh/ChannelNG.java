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
package com.sshtools.common.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Vector;

import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.IdleStateListener;
import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.sshd.SshMessage;

/**
 * This abstract class provides the basic functions of an SSH2 channel. All
 * terminal sessions, forwarded connections etc, are channels. Either side may
 * open a channel and multiple channels are multiplexed into a single
 * connection. Channels are flow-controlled so that no data may be sent to a
 * channel until a message is received to indicate that a window space is
 * available.
 */
public abstract class ChannelNG<T extends SshContext> implements Channel {

	final static int CHANNEL_UNINITIALIZED = 0;
	final static int CHANNEL_OPEN = 1;
	final static int CHANNEL_CLOSED = 2;

	long lastActivity = System.currentTimeMillis();
	int timeout = 0;
	
	/**
	 * The Connection Protocol instance managing this session, use this instance
	 * to disconnect the session or obtain the ip address of the remote client.
	 */
	protected ConnectionProtocol<T> connection;

	String channeltype;
	int channelid;
	int remoteid;
	
	protected ChannelDataWindow localWindow;
	protected ChannelDataWindow remoteWindow;
	
	boolean isLocalEOF = false;
	boolean isRemoteEOF = false;
	boolean sentEOF = false;
	boolean sentClose = false;
	boolean receivedClose = false;
	boolean completedClose = false;
	
	boolean haltIncomingData = false;
	
	int state = CHANNEL_UNINITIALIZED;

	Vector<ChannelEventListener> eventListeners = new Vector<ChannelEventListener>();
	
	ChannelRequestFuture openFuture = new ChannelRequestFuture();
	LinkedList<ChannelRequestFuture> requests = new LinkedList<ChannelRequestFuture>();
	ChannelRequestFuture closeFuture;
	
	protected final SshConnection con;
	
	/**
	 * Construct a channel with the specified settings.
	 * 
	 * @param channelType
	 *            the name of the channel, for example "session" or
	 *            "tcpip-forward"
	 * @param maximumPacketSize
	 *            the maximum size of an individual packet that the remote side
	 *            can send through the channel
	 * @param initialWindowSize
	 *            the initial size of the local window.
	 */
	public ChannelNG(String channelType, SshConnection con, int maximumPacketSize,
			int initialWindowSize, int maximumWindowSpace, int minimumWindowSpace, ChannelRequestFuture closeFuture) {
		this.channeltype = channelType;
		this.con = con;
		this.localWindow = createLocalWindow(initialWindowSize, maximumWindowSpace, minimumWindowSpace, maximumPacketSize);
		this.closeFuture = closeFuture;
	}

	public ChannelNG(String channelType, SshConnection con, int maximumPacketSize,
			int initialWindowSize, int maximumWindowSpace, int minimumWindowSpace) {
		this(channelType, con, maximumPacketSize,
				initialWindowSize, maximumWindowSpace, 
				minimumWindowSpace, new ChannelRequestFuture());
	}
	
	protected ChannelDataWindow createLocalWindow(int initialWindowSize, int maximumWindowSpace, int minimumWindowSpace, int maximumPacketSize) {
		return new ChannelDataWindow(initialWindowSize, maximumWindowSpace, minimumWindowSpace, maximumPacketSize);
	}
	
	/**
	 * Indicates the channel has been closed
	 * 
	 * @return boolean
	 */
	public boolean isClosed() {
		return state == CHANNEL_CLOSED;
	}

	public int getMaxiumRemoteWindowSize() {
		return remoteWindow.getMaximumWindowSpace();
	}
	
	public int getMaxiumRemotePacketSize() {
		return remoteWindow.getMaximumPacketSize();
	}
	
	/**
	 * Indicates that the channel is EOF (it will not be receiving any more data
	 * from the remote side).
	 * 
	 * @return boolean
	 */
	public boolean isEOF() {
		return isRemoteEOF;
	}

	void init(ConnectionProtocol<T> connection) {
		this.connection = connection;
	}

	/**
	 * Allows a channel to register and receive idle state events. Call this
	 * method to reset the idle state of a channel (i.e when activity has
	 * occurred). The {@link IdleStateListener} instance passed as a parameter
	 * will receive notification once the channel reaches an idle state.
	 * 
	 * NOTE: It is the callers responsibility to call clearIdleState method to
	 * ensure references are released.
	 * 
	 * @param listener
	 *            IdleStateListener
	 */
	public void resetIdleState(IdleStateListener listener) {
		connection.transport.getSocketConnection().getIdleStates()
				.register(listener);
	}

	/**
	 * Clear an idle state listener.
	 * 
	 * @param listener
	 */
	public void clearIdleState(IdleStateListener listener) {
		connection.transport.getSocketConnection().getIdleStates()
				.remove(listener);
	}

	/**
	 * Enable other objects to receive channel events
	 * 
	 * @param listener
	 */
	public void addEventListener(ChannelEventListener listener) {
		if (listener != null) {
			eventListeners.add(listener);
		}
	}

	/**
	 * The name of this channel.
	 * 
	 * @return String
	 */
	public String getChannelType() {
		return channeltype;
	}

	/**
	 * Get this channels close future.
	 * @return
	 */
	public ChannelRequestFuture getOpenFuture() {
		return openFuture;
	}
	
	/**
	 * Get this channels close future.
	 * @return
	 */
	public ChannelRequestFuture getCloseFuture() {
		return closeFuture;
	}
	
	/**
	 * The current size of the remote data window.
	 * 
	 * @return int
	 */
	public int getRemoteWindow() {
		return remoteWindow.getWindowSpace();
	}

	/**
	 * The current size of the local data window.
	 * 
	 * @return int
	 */
	public int getLocalWindow() {
		return localWindow.getWindowSpace();
	}

	/**
	 * The maximum size of a single packet that the local side will accept.
	 * 
	 * @return int
	 */
	public int getLocalPacket() {
		return localWindow.getMaximumPacketSize();
	}

	/**
	 * The maximum size of a single packet that the remote side will accept.
	 * 
	 * @return int
	 */
	public int getRemotePacket() {
		return remoteWindow.getMaximumPacketSize();
	}

	/**
	 * The local channel id
	 * 
	 * @return int
	 */
	public int getLocalId() {
		return channelid;
	}

	/**
	 * The remote sides channel id
	 * 
	 * @return int
	 */
	public int getRemoteId() {
		return remoteid;
	}

	/**
	 * Open a channel.
	 * 
	 * @param channelid
	 *            the local channel id
	 * @param remoteid
	 *            the remote channel id
	 * @param remotepacket
	 *            the remotes maximum packet size
	 * @param remotewindow
	 *            the remotes intial window
	 * @param requestdata
	 * @return byte[]
	 * @throws WriteOperationRequest
	 * @throws ChannelOpenException
	 */
	byte[] open(int channelid, int remoteid, int remotepacket,
			int remotewindow, byte[] requestdata) throws WriteOperationRequest,
			ChannelOpenException {

		this.channelid = channelid;
		this.remoteid = remoteid;

		this.remoteWindow = new ChannelDataWindow(remotewindow, remotewindow, 0, remotepacket);

		return openChannel(requestdata);
	}

	void confirmOpen() {
		state = CHANNEL_OPEN;
		openFuture.done(true);
		onChannelOpenConfirmation();

		for (ChannelEventListener listener : eventListeners) {
			listener.onChannelOpen(this);
		}

	}

	void confirmOpen(int remoteid, int remotewindow, int remotepacket) {
		this.remoteid = remoteid;
		this.remoteWindow = new ChannelDataWindow(remotewindow, remotewindow, 0, remotepacket);
		confirmOpen();
	}

	/**
	 * Get the session id for the current connection.
	 * 
	 * @return byte[]
	 */
	public String getSessionIdentifier() {
		return connection.getSessionIdentifier();
	}

	void adjustWindow(int count) {
		
		remoteWindow.adjust(count);
		
		synchronized(ChannelNG.this) {
			ChannelNG.this.notifyAll();
		}
		
		onWindowAdjust(count);

		for (ChannelEventListener listener : eventListeners) {
			listener.onWindowAdjust(this, remoteWindow.getWindowSpace());
		}

	}
	
	protected void registerExtendedDataType(Integer type) {
	}

	protected void onWindowAdjust(int count) {
	}

	public long getLastActivity() {
		return lastActivity;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	public Connection<T> getConnection() {
		return connection.getConnection();
	}
	
	/**
	 * Returns the {@link ConnectionProtocol} associated with this channel.
	 * 
	 * @return ConnectionProtocol
	 */
	public ConnectionProtocol<T> getConnectionProtocol() {
		return connection;
	}

	void consumeWindowSpace(int length) throws IOException {

		synchronized (localWindow) {

			if (localWindow.getWindowSpace() < length) {
				throw new IOException("Data length of "
						+ String.valueOf(length)
						+ " bytes exceeded available window space of "
						+ String.valueOf(localWindow.getWindowSpace()) + " bytes.");
			}

			if(Log.isTraceEnabled()) {
				log("Consuming", length
						+ " bytes local window space before=" + localWindow.getWindowSpace() + " after="
						+ (localWindow.getWindowSpace() - length));
			}
			localWindow.consume(length);
		}
	}

	void processChannelData(ByteBuffer data) throws IOException {
		

		lastActivity = System.currentTimeMillis();
		
		if(Log.isDebugEnabled()) {
			log("Received", String.format("SSH_MSG_CHANNEL_DATA len=%d", data.remaining()));
		}
		
		// We have new data so reduce the available window space
		consumeWindowSpace(data.remaining());
		
		// Process the data
		onChannelData(data);

	}

	/**
	 * Send channel data to the remote side of the channel. The byte array
	 * passed into this method will be sent asynchronously so ensure that no
	 * other class has access to this after this method returns, failing to do
	 * so may cause data corruption.
	 * 
	 * @param data
	 */
	public void sendChannelDataAndBlock(byte[] data) throws IOException {
		sendChannelDataAndBlock(data, null);
	}

	public void sendChannelDataAndBlock(byte[] data, Runnable r) throws IOException {
		sendChannelDataAndBlock(data, 0, data.length, r);
	}
	
	public void sendData(byte[] data, int off, int len) throws IOException {
		sendChannelDataAndBlock(data, off, len, null);
	}

	public void sendChannelDataAndBlock(byte[] data, int off, int len, Runnable r) throws IOException {
		
		lastActivity = System.currentTimeMillis();
		sendChannelDataAndBlock(ByteBuffer.wrap(data, off, len), r);
    	
	}
	
	/**
	 * Send channel data from a ByteBuffer
	 * @param buf
	 */
	public void sendChannelDataAndBlock(ByteBuffer buf) throws IOException {
		sendChannelDataAndBlock(buf, 0, null);
	}
	
	
	public void sendChannelDataAndBlock(ByteBuffer buf, Runnable r) throws IOException {
		sendChannelDataAndBlock(buf, 0, r);
	}
	/**
	 * Send channel data from a ByteBuffer
	 * @param buf
	 * @param r
	 */
	public void sendChannelDataAndBlock(ByteBuffer buf, int type, Runnable r) throws IOException {
		
		if(getConnectionProtocol().getTransport().getSocketConnection().isSelectorThread()) {
			throw new IllegalStateException("You appear to be calling sendChannelData on a selector thread. Use TransportProtocol.addOutgoingTask to place on the outgoing message queue.");
		}
		
		lastActivity = System.currentTimeMillis();
		
		ChannelData lastMessage = null;

		if(Log.isTraceEnabled()) {
			Log.debug(String.format("Queue Buffer rem=%d pos=%d limit=%d, capacity=%d", buf.remaining(), buf.position(), buf.limit(), buf.capacity()));
		}
		
		synchronized(ChannelNG.this) {

			do {
			
				if(isLocalEOF || isClosed()) {
					throw new IOException("Channel has been closed");
				}
				
				int count = Math.min(remoteWindow.getMaximumPacketSize(), 
						Math.min(remoteWindow.getWindowSpace(), buf.remaining()));
				
				if(count == 0) {
					if(Log.isDebugEnabled()) {
						log("Waiting", String.format("for %d bytes of remote window", buf.remaining()));
					}
					try {
						wait(5000);
					} catch (InterruptedException e) {
					}

					continue;
				}	

				remoteWindow.consume(count);

				if(buf.remaining() > count) {
					ByteBuffer processedBuffer = buf.slice();
					processedBuffer.limit(count);
					buf.position(buf.position() + count);
			
					if(Log.isTraceEnabled()) {
						Log.trace(String.format("Sliced Buffer rem=%d pos=%d limit=%d, capacity=%d", 
								processedBuffer.remaining(), processedBuffer.position(), 
								processedBuffer.limit(), processedBuffer.capacity()));
					}
					connection.sendMessage(new ChannelData(processedBuffer, type, remoteWindow.getWindowSpace()));
				} else {
					
					if(Log.isTraceEnabled()) {	
						Log.trace(String.format("Final Buffer rem=%d pos=%d limit=%d, capacity=%d", 
								buf.remaining(), buf.position(), buf.limit(), buf.capacity()));
					}
					connection.sendMessage(lastMessage = new ChannelData(buf, type, remoteWindow.getWindowSpace()));
				}
			
				
			} while(Objects.isNull(lastMessage));
			
		}
		
		if(!Objects.isNull(lastMessage)) {
			synchronized(lastMessage) {
				long t = System.currentTimeMillis();
				while(!isClosed() && !lastMessage.isMessageSent() && System.currentTimeMillis() - t < 120000) {
					if(Log.isTraceEnabled()) {
						Log.trace("Waiting for sent data notification");
					}
					try {
						lastMessage.wait(1000);
					} catch (InterruptedException e) {
					}
				}
				if(!lastMessage.isMessageSent()) {
					throw new IOException("Timeout waiting for data to be sent on channel " + getLocalId());
				}
				if(Log.isTraceEnabled()) {
					Log.trace("Received sent data notification");
				}
			}
		}

		if(r!=null) {
			getConnectionProtocol().addTask(ExecutorOperationSupport.CALLBACKS, new ConnectionTaskWrapper(getConnection(), r));
		}
	}
	
	/**
	 * Get the current configuration from the underlying connection.
	 * 
	 * @return ConfigurationContext
	 */
	public T getContext() {
		return connection.getContext();
	}

	/**
	 * Send extended channel data. This data is sent as an extended 'type' which
	 * should be known by the channel at the remote side. For example within a
	 * session channel an extended data type is used to transfer stderr data
	 * from the server to the client.
	 * 
	 * @param data
	 * @param type
	 */
	protected void sendExtendedData(byte[] data, int type) throws IOException {
		sendExtendedData(data, 0, data.length, type);
	}

	/**
	 * Send extended channel data. This data is sent as an extended 'type' which
	 * should be known by the channel at the remote side. For example within a
	 * session channel an extended data type is used to transfer stderr data
	 * from the server to the client.
	 * 
	 * @param data
	 * @param off
	 * @param len
	 * @param type
	 */
	protected void sendExtendedData(byte[] data, int off, int len, int type) throws IOException {
		sendChannelDataAndBlock(ByteBuffer.wrap(data, off, len), type, null);
	}

	/**
	 * Called by the channel when data arrives from the remote side.
	 * 
	 * @param data
	 */
	protected abstract void onChannelData(ByteBuffer data);

	void processExtendedData(int type, ByteBuffer data) throws IOException {

		if(Log.isDebugEnabled()) {
			log("Received", String.format("SSH_MSG_CHANNEL_EXTENDED_DATA len=%d type=%d", data.remaining(), type));
		}

		consumeWindowSpace(data.remaining());
		
		onExtendedData(data, type);
	}

	/**
	 * Called by the channel when extended data arrives
	 * 
	 * @param data
	 */
	protected abstract void onExtendedData(ByteBuffer data, int type);

	void processChannelEOF() {
		
		for (ChannelEventListener listener : eventListeners) {
			listener.onChannelEOF(this);
		}
		
		isRemoteEOF = true;
		onRemoteEOF();
	}

	void processChannelClose() {

		receivedClose = true;
		isRemoteEOF = true;
		onRemoteClose();
	}

	/**
	 * <p>
	 * Send a channel request. Many channel types have extensions that are
	 * specific to that particular channel type. An example is requresting a pty
	 * (pseudo terminal) for an interactive session. This method enables the
	 * sending of channel requests but does not support receiving responses.
	 * This should not present a problem as server implementations tend to send
	 * messages as one way information, for example the exit-status request of
	 * an interactive session.
	 * </p>
	 * 
	 * <p>
	 * To handle requests from a client implement
	 * {@link #onChannelRequest(String,boolean,byte[])}.
	 * </p>
	 * 
	 * 
	 * @param type
	 * @param wantreply
	 * @param requestdata
	 */
	public void sendChannelRequest(String type, boolean wantreply,
			byte[] requestdata, ChannelRequestFuture future) {
		if(!wantreply) {
			future.done(true);
		} else {
			requests.addLast(future);
		}
		connection.sendMessage(new ChannelRequest(type, wantreply, requestdata));
	}
	
	public void sendChannelRequest(String type, boolean wantreply,
			byte[] requestdata) {
		if(wantreply) {
			requests.addLast(new ChannelRequestFuture());
		}
		connection.sendMessage(new ChannelRequest(type, wantreply, requestdata));
	}
	
	protected void processChannelRequestResponse(boolean success) {
		
		ChannelRequestFuture future = requests.removeFirst();
		
		if(Log.isDebugEnabled()) {
			log("Received", (success ? "SSH_MSG_CHANNEL_SUCCESS" : "SSH_MSG_CHANNEL_FAILURE"));
		}
		
		future.done(success);
	}

	void fail() {
		openFuture.done(false);
		onChannelOpenFailure();
	}

	/**
	 * Called when the remote side fails to open a channel in response to our
	 * request.
	 * 
	 * It only makes sense for some channel types to override this method. A
	 * local forwarding channel will never receive this message for example.
	 */
	protected void onChannelOpenFailure() {
	}

	/**
	 * Called when the remote side closes the channel. Override this method to
	 * change the default behaviour of instantly closing the channel unless
	 * there is buffered data remaining
	 */
	protected void onRemoteClose() {
		close();
	}

	/**
	 * Indicates whether the channel is currently performing a close operation
	 * 
	 * @return boolean
	 */
	public boolean isClosing() {
		return sentClose;
	}

	public void close() {
		close(false);
	}

	/**
	 * This method closes the channel and free's its resources.
	 */
	void close(boolean forceClose) {

		if(Log.isTraceEnabled()) {
				log("Checking", "close state force="
						+ forceClose + " channelType=" + getChannelType());
		}

		boolean doSend = false;
		synchronized(ChannelNG.this) {
			boolean canClose = forceClose || canClose();
			if (!sentClose && canClose) {

				sentClose = true;
				doSend = true;
				
				for (ChannelEventListener listener : eventListeners) {
					listener.onChannelClosing(this);
				}

				onChannelClosing();

				if(Log.isTraceEnabled()) {
						log("Adding", "our close message to queue");
				}
				
				state = CHANNEL_CLOSED;

				notifyAll();

			} 
		}

		if (doSend && connection.isConnected()) {
			connection.sendMessage(new ChannelClose(receivedClose));
		}
		
		if (!connection.isConnected() || forceClose) {
			if(Log.isTraceEnabled()) {
					log("Requesting", "to complete the close operation connected="
							+ connection.isConnected() + " forceClose="
							+ forceClose);
			}
			
			if(forceClose) {
				
				for (ChannelEventListener listener : eventListeners) {
					listener.onChannelDisconnect(this);
				}
				
				onChannelDisconnect();
			}
			
			completeClose();
		} else if (receivedClose) {
			if(Log.isTraceEnabled()) {
					log("We've", "already received the remote close message");
			}
			if (sentClose) {
				if(Log.isTraceEnabled()) {
						log("We've", "already sent our close message");
				}
				completeClose();
			}
		}

	}

	private void completeClose() {

		connection.addTask(ExecutorOperationSupport.CALLBACKS, new ConnectionTaskWrapper(getConnection(), new Runnable() {
			public void run() {
				boolean hasPerformedClose = false;
				synchronized (ChannelNG.this) {
					hasPerformedClose = !completedClose;

					if (!completedClose) {
						if(Log.isTraceEnabled()) {
							log("Completing", "the close operation");
						}
						for (ChannelEventListener listener : eventListeners) {
							listener.onChannelClose(ChannelNG.this);
						}

						onChannelClosed();
						completedClose = true;
						ChannelNG.this.notifyAll();
					}
				}

				if (hasPerformedClose) {
					closeFuture.done(true);
					connection.freeChannel(ChannelNG.this);
					free();
				}
			}
		}));
		
	}

	/**
	 * This method is called when the channel has been closed and all its
	 * resources are no longer required.
	 */
	protected abstract void onChannelFree();

	private void free() {
		if (connection != null) {
			if(Log.isTraceEnabled()) { 
				log("Freeing" ,"channel");
			}
		}

		if (eventListeners != null) {
			eventListeners.clear();
		}

		onChannelFree();
	}

	byte[] create(int channelid) throws IOException {
		this.channelid = channelid;
		return createChannel();
	}

	/**
	 * Called when the channel is being created. You can return data to be sent
	 * in the channel open request, or null for none.
	 * 
	 * @return byte[]
	 * @throws IOException
	 */
	protected abstract byte[] createChannel() throws IOException;

	/**
	 * Called when the channel is being opened. You can retrun data to be sent
	 * in the channel open confirmation message, or null for none.
	 * 
	 * @param requestdata
	 * @return byte[]
	 * @throws WriteOperationRequest
	 * @throws ChannelOpenException
	 * @throws  
	 */
	protected abstract byte[] openChannel(byte[] requestdata)
			throws WriteOperationRequest, ChannelOpenException;

	/**
	 * Called when the channel has been confirmed as open by the remote side -
	 * this method is only called when the channel has been requested by this
	 * side of the connection
	 */
	protected abstract void onChannelOpenConfirmation();

	/**
	 * Called when the channel has been closed to enable resources to be freed.
	 */
	protected abstract void onChannelClosed();

	/**
	 * Called when the channel has been opened - this method is only called when
	 * the remote side requests a channel open.
	 */
	protected abstract void onChannelOpen();

	/**
	 * Called before the channel is closed
	 */
	protected abstract void onChannelClosing();

	/**
	 * Called when a channel request is received.
	 * 
	 * @param type
	 * @param wantreply
	 * @param requestdata
	 */
	protected abstract void onChannelRequest(String type, boolean wantreply,
			byte[] requestdata);

	/**
	 * Called to evaluate the window space available. Send a window adjust
	 * message if there if the minimum amount of space is not available.
	 * 
	 * @param remaining
	 */
	protected void evaluateWindowSpace() {
		synchronized (localWindow) {
			if (localWindow.isAdjustRequired() && isOpen() && !haltIncomingData ) {
				sendWindowAdjust();
			}
		}
	}

	/**
	 * Called when the remote side reports its OutputStream as EOF.
	 */
	protected abstract void onRemoteEOF();

	/**
	 * You can send EOF when you have no more data to send. The channel will
	 * still remain open until a close message is received.
	 */
	public void sendEOF() {

		if (isOpen() && !sentClose && !isLocalEOF) {
			isLocalEOF = true;
			remoteWindow.close();
			connection.sendMessage(new ChannelEOF());
			onLocalEOF();
		}
	}

	/**
	 * Can this channel close?
	 * 
	 * @return true if there is no queued data, else false
	 */
	protected synchronized boolean canClose() {
		return true;
	}

	/**
	 * Called when the local side is EOF.
	 */
	protected abstract void onLocalEOF();

	/**
	 * Get the open state of the channel.
	 * 
	 * @return boolean
	 */
	protected boolean isOpen() {
		return state == CHANNEL_OPEN;
	}

	/**
	 * Send a channel request.
	 * 
	 * @param succeeded
	 */
	protected void sendRequestResponse(boolean succeeded) {
		if (succeeded) {
			connection.sendMessage(new RequestSuccess());
		} else {
			connection.sendMessage(new RequestFailure());
		}
	}

	/**
	 * Adjust the local window by adding more bytes.
	 * 
	 * @param count
	 */
	protected void sendWindowAdjust() {

		synchronized (localWindow) {
			int count = localWindow.getAdjustCount();
			sendWindowAdjust(count);	
		}
	}

	public void sendWindowAdjust(int count) {
		synchronized (localWindow) {
			if(Log.isTraceEnabled()) {
				log("Increasing", "window space by " + String.valueOf(count) + " bytes");
			}
			connection.sendMessage(new WindowAdjust(this, count, localWindow.getWindowSpace()));
			localWindow.adjust(count);		
		}
	}

	class ChannelRequest implements SshMessage {

		String type;
		boolean wantreply;
		byte[] requestdata;

		ChannelRequest(String type, boolean wantreply, byte[] requestdata) {
			this.type = type;
			this.wantreply = wantreply;
			this.requestdata = requestdata;
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {

			try {
				buf.put((byte) ConnectionProtocol.SSH_MSG_CHANNEL_REQUEST);
				buf.putInt(remoteid);
				buf.putInt(type.length());
				buf.put(type.getBytes(TransportProtocol.CHARSET_ENCODING));
				buf.put((byte) (wantreply ? 1 : 0));
				if (requestdata != null) {
					buf.put(requestdata);
				}
			} catch (UnsupportedEncodingException ex) {
				connection.close(TransportProtocol.PROTOCOL_ERROR,
						"Could not encode string using "
								+ TransportProtocol.CHARSET_ENCODING
								+ " charset");
			}

			return true;

		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled()) {
				logMessage(String.format("SSH_MSG_CHANNEL_REQUEST request=%s wantReply=%s", type, String.valueOf(wantreply)));
			}
		}

	}

	class WindowAdjust implements SshMessage {

		long count;
		ChannelNG<T> channel;
		long window;

		WindowAdjust(ChannelNG<T> channel, long count, long window) {
			this.channel = channel;
			this.count = count;
			this.window = window;
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put((byte) ConnectionProtocol.SSH_MSG_CHANNEL_WINDOW_ADJUST);
			buf.putInt(remoteid);
			buf.putInt((int)count);
			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled()) {
				logMessage(String.format("SSH_MSG_CHANNEL_WINDOW_ADJUST count=%d window=%d", count, window));
			}
		}
	}

	class RequestSuccess implements SshMessage {
		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put((byte) ConnectionProtocol.SSH_MSG_CHANNEL_SUCCESS);
			buf.putInt(remoteid);
			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled()) {
				logMessage("SSH_MSG_CHANNEL_SUCCESS");
			}
		}
	}

	class RequestFailure implements SshMessage {
		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put((byte) ConnectionProtocol.SSH_MSG_CHANNEL_FAILURE);
			buf.putInt(remoteid);
			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled()) {
				logMessage("SSH_MSG_CHANNEL_FAILURE");
			}
		}

	}

	static int sequence = 0;

	class ChannelData implements SshMessage {

		int sequenceNo = sequence++;
		ByteBuffer msg;
		int type;
		int count;
		int remoteWindow;
		boolean sent;
		
		ChannelData(ByteBuffer msg, int type, int remoteWindow) {
			this.msg = msg;
			this.type = type;
			this.remoteWindow = remoteWindow;
			this.count = msg.remaining();
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {

			/*
			 * byte SSH_MSG_CHANNEL_DATA uint32 recipient channel string data
			 */
			if(type >= 0x0000001) {
				buf.put((byte) ConnectionProtocol.SSH_MSG_CHANNEL_EXTENDED_DATA);
				buf.putInt(remoteid);
				buf.putInt(type);
			} else {
				buf.put((byte) ConnectionProtocol.SSH_MSG_CHANNEL_DATA);
				buf.putInt(remoteid);
			}
			
			buf.putInt(count);
			buf.put(msg);
			
			msg = null; 

			return true;
		}

		public synchronized void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled()) {
				logMessage(String.format("%s seq=%d len=%d", 
						type > 0 ? "SSH_MSG_CHANNEL_EXTENDED_DATA" : "SSH_MSG_CHANNEL_DATA", 
								sequenceNo, count));
			}
			
//			if(waitingToClose) {
//				close();
//			}
			sent = true;
			notifyAll();
		}

		public synchronized boolean isMessageSent() {
			return sent;
		}
	}

	protected void logMessage(String message) {
		log("Sent", message);
	}
	
	protected void log(String action, String message) {
		Log.debug(String.format("%s %s channel=%d remote=%d",
				action,
				message,
				channelid, 
				remoteid));
	}
	
	protected void log(String message) {
		Log.debug(String.format("%s channel=%d remote=%d", 
				message,
				channelid, 
				remoteid));
	}
	
	protected void log(String message, Throwable t) {
		Log.debug(String.format("%s channel=%d remote=%d",  
				message,
				channelid, 
				remoteid), t);
	}
	
	class ChannelClose implements SshMessage {
		boolean finish;

		ChannelClose(boolean finish) {
			this.finish = finish;
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			/*
			 * byte SSH_MSG_CHANNEL_CLOSE uint32 recipient channel
			 */
			buf.put((byte) ConnectionProtocol.SSH_MSG_CHANNEL_CLOSE);
			buf.putInt(remoteid);
			return true;
		}

		public void messageSent(Long sequenceNo) {

			if (finish)
				completeClose();

			if(Log.isDebugEnabled()) {
				logMessage("SSH_MSG_CHANNEL_CLOSE");
			}

		}
	}

	class ChannelEOF implements SshMessage {
		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			/*
			 * byte SSH_MSG_CHANNEL_EOF uint32 recipient channel
			 */
			buf.put((byte) ConnectionProtocol.SSH_MSG_CHANNEL_EOF);
			buf.putInt(remoteid);
			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled()) {
				logMessage("SSH_MSG_CHANNEL_EOF");
			}
		}
	}

	public boolean isLocalEOF() {
		return isLocalEOF;
	}
	
	public boolean isRemoteEOF() {
		return isRemoteEOF;
	}

	void log() {
		if(Log.isInfoEnabled()) {
			Log.info(String.format("Channel id=%d type=%s localEOF=%s remoteEOF=%s sentClose=%s receivedClose=%s completedClose=%s remoteWindow=%d localWindow=%d",
						getLocalId(), getChannelType(), isLocalEOF, isRemoteEOF, sentClose, receivedClose, completedClose, getRemoteWindow(), getLocalWindow()));
		}
	}
	
	public boolean isIncomingDataHalted() {
		synchronized(localWindow) {
			return haltIncomingData;
		}
    }

	public void haltIncomingData() {
		synchronized(localWindow) {
			haltIncomingData = true;
		}
	}

	public void resumeIncomingData() {
		synchronized (localWindow) {
			haltIncomingData = false;
			evaluateWindowSpace();
		}
	}

	protected abstract boolean checkWindowSpace();
	
	protected class ChannelInputStream extends InputStream {

		CachingDataWindow cache;
		boolean streamClosed;
		
		public ChannelInputStream(CachingDataWindow cache) {
			this.cache = cache;
		}
		
		@Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			int r = read(b);
			if(r > 0) {
				int res = b[0] & 0xFF; 
				if(Log.isTraceEnabled()) {
					Log.trace("Read returning %d", res);
				}
				return res;
			}
			return -1;
		}

		@Override
		public void close() throws IOException {
			if(!streamClosed) {
				streamClosed = true;
				synchronized(cache) {
					cache.notify();
				}
			}
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			
			long start = System.currentTimeMillis();
			
			synchronized (localWindow) {
				
				if(checkWindowSpace()) {
					sendWindowAdjust();
				}
				
				while(!cache.hasRemaining() 
						&& (timeout==0 
							|| (System.currentTimeMillis() - start) < timeout)) {
				
					if(streamClosed || isClosed() || isRemoteEOF()) {
						return -1;
					}
					
					try {
						cache.waitFor(1000);
					} catch (InterruptedException e) {
					}
				}
				
				if(!cache.hasRemaining()) {
					if(streamClosed || isClosed() || isRemoteEOF()) {
						return -1;
					}
					throw new InterruptedIOException("No data received within the timeout threshold");
				}
				
				
				int r = cache.get(ByteBuffer.wrap(b, off, len));
				
				if(checkWindowSpace()) {
					sendWindowAdjust();
				}

				return r;
			}
		
		}
	}

	protected void onChannelDisconnect() {
		// TODO Auto-generated method stub
		
	}
}
