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

package com.sshtools.synergy.ssh;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.ExecutorOperationQueues;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.synergy.ssh.GlobalRequestHandler.GlobalRequestHandlerException;

/**
 * This class implements the SSH Connection Protocol as an SSH Transport
 * Protocol service.
 */
public abstract class ConnectionProtocol<T extends SshContext> 
		extends ExecutorOperationSupport<SshContext> implements Service {

	
	private static final Integer CHANNEL_DATA_IN = ExecutorOperationQueues.generateUniqueQueue("ConnectionProtocol.channelDataIn");

	TransportProtocol<T> transport;
	
	final static int SSH_MSG_GLOBAL_REQUEST = 80;
	final static int SSH_MSG_GLOBAL_REQUEST_SUCCESS = 81;
	final static int SSH_MSG_GLOBAL_REQUEST_FAILURE = 82;

	final static int SSH_MSG_CHANNEL_OPEN = 90;
	final static int SSH_MSG_CHANNEL_OPEN_CONFIRMATION = 91;
	final static int SSH_MSG_CHANNEL_OPEN_FAILURE = 92;

	final static int SSH_MSG_CHANNEL_WINDOW_ADJUST = 93;
	final static int SSH_MSG_CHANNEL_DATA = 94;
	final static int SSH_MSG_CHANNEL_EXTENDED_DATA = 95;
	final static int SSH_MSG_CHANNEL_EOF = 96;
	final static int SSH_MSG_CHANNEL_CLOSE = 97;

	final static int SSH_MSG_CHANNEL_REQUEST = 98;
	final static int SSH_MSG_CHANNEL_SUCCESS = 99;
	final static int SSH_MSG_CHANNEL_FAILURE = 100;

	public final static String SERVICE_NAME = "ssh-connection";

	Set<Integer> channeIdPool = new HashSet<Integer>();
	Map<Integer,ChannelNG<T>> activeChannels = new ConcurrentHashMap<Integer, ChannelNG<T>>(8, 0.9f, 1);
	Map<String, GlobalRequestHandler<T>> globalRequestHandlers = new ConcurrentHashMap<String, GlobalRequestHandler<T>>(8, 0.9f, 1);
	
	protected LinkedList<GlobalRequest> outstandingRequests = new LinkedList<GlobalRequest>();
	
	protected String username;
	protected Connection<T> con;
	
	public ConnectionProtocol(TransportProtocol<T> transport, String username) {
		super("connection-protocol");
		this.username = username;
		this.transport = transport;
		this.con = transport.getConnection();
		
		for(int i=0;i<transport.getSshContext().getChannelLimit();i++) {
			channeIdPool.add(Integer.valueOf(i));
		}

		if(Log.isDebugEnabled())
			Log.debug("Initialized MaxChannels="
					+ String.valueOf(transport.getSshContext()
							.getChannelLimit()));

	}

	public void addGlobalRequestHandler(GlobalRequestHandler<T> handler) {
		if (handler != null) {
			String[] requests = handler.supportedRequests();
			for (int i = 0; i < requests.length; i++) {
				globalRequestHandlers.put(requests[i], handler);
			}
		}
	}

	/**
	 * Get the address of the remote client.
	 * 
	 * @return SocketAddress
	 */
	public SocketAddress getRemoteAddress() {
		return transport.getSocketConnection().getRemoteAddress();
	}

	/**
	 * Get the local address to which the remote socket is connected.
	 * 
	 * @return InetAddress
	 */
	public SocketAddress getLocalAddress() {
		return transport.getSocketConnection().getLocalAddress();
	}

	/**
	 * Get the local port ro which the remote socket is connected.
	 * 
	 * @return int
	 */
	public int getLocalPort() {
		return transport.getSocketConnection().getLocalPort();
	}

	/**
	 * Get the username for the connected user.
	 * 
	 * @return String
	 */
	public String getUsername() {
		return username;
	}

	protected abstract void onStop();
	
	public void stop() {

		onStop();

		if (activeChannels != null) {
			if(Log.isDebugEnabled())
				Log.debug("Cleaning up connection protocol references");

			synchronized (activeChannels) {
				for (ChannelNG<T> channel : activeChannels.values()) {
					try {
						channel.close(true);
					} catch (Throwable t) {
					}
				}
			}
		}
	}

	public String getSessionIdentifier() {
		return transport.getUUID();
	}

	int allocateChannel(ChannelNG<T> channel) {

		synchronized (activeChannels) {
			if(channeIdPool.size()==0) {
				return -1;
			}
			Integer channelId = channeIdPool.iterator().next();
			channeIdPool.remove(channelId);
			activeChannels.put(channelId, channel);
			return channelId;
		}
	}

	void freeChannel(ChannelNG<T> channel) {
		synchronized (activeChannels) {
			if (channel != null) {
				if(Log.isDebugEnabled())
					Log.debug("Freeing channel="
							+ String.valueOf(channel.getLocalId()));
				Integer channelId = channel.getLocalId();
				activeChannels.remove(channelId);
				channeIdPool.add(channelId);
			}
		}
	}

	public void openChannel(ChannelNG<T> channel) {

		channel.init(this);

		synchronized (channel) {

			try {
				int channelid = allocateChannel(channel);
				if (channelid == -1) {
					
					if(Log.isDebugEnabled()) {
						Log.debug("Failed to allocate channel {}", 
								channel.getChannelType());
					}
					channel.getOpenFuture().done(false);

				}

				transport.postMessage(new ChannelOpenMessage(
						channel, channel.create(channelid)));

				/*
				 * try { channel.wait(); } catch (InterruptedException ex) { }
				 */
			} catch (IOException ex1) {
				if(Log.isDebugEnabled()) {
					Log.debug("Failed to open channel {}", ex1, channel.getChannelType());
				}
				channel.getOpenFuture().done(false);
			}
		}
	}

	boolean isConnected() {
		return transport.isConnected();
	}

	void sendMessage(final SshMessage msg) {
		transport.postMessage(msg);
	}

	public List<ChannelNG<T>> getActiveChannels() {
		return new ArrayList<ChannelNG<T>>(activeChannels.values());
	}

	public int getMaxChannels() {
		return transport.getSshContext().getChannelLimit();
	}

	/**
	 * Disconnect the current connection.
	 */
	public void disconnect() {
		close(TransportProtocol.BY_APPLICATION, "User Disconnected");
	}

	void close(int reason, String description) {
		transport.disconnect(reason, description);
	}

	@Override
	public boolean processMessage(byte[] msg) throws IOException {

		switch (msg[0]) {

		case SSH_MSG_CHANNEL_OPEN:
			processChannelOpen(msg);
			return true;
		case SSH_MSG_CHANNEL_OPEN_CONFIRMATION:
			processChannelOpenConfirmation(msg);
			return true;
		case SSH_MSG_CHANNEL_OPEN_FAILURE:
			processChannelOpenFailure(msg);
			return true;
		case SSH_MSG_CHANNEL_REQUEST:
			processChannelRequest(msg);
			return true;
		case SSH_MSG_CHANNEL_DATA:
			processChannelData(msg);
			return true;
		case SSH_MSG_CHANNEL_EXTENDED_DATA:
			processChannelData(msg);
			return true;
		case SSH_MSG_CHANNEL_WINDOW_ADJUST:
			processChannelWindowAdjust(msg);
			return true;
		case SSH_MSG_CHANNEL_EOF:
			processChannelEOF(msg);
			return true;
		case SSH_MSG_CHANNEL_CLOSE:
			processChannelClose(msg);
			return true;
		case SSH_MSG_GLOBAL_REQUEST:
			processGlobalRequest(msg);
			return true;
		case SSH_MSG_GLOBAL_REQUEST_FAILURE:
			processGlobalRequestFailure(msg);
			return true;
		case SSH_MSG_GLOBAL_REQUEST_SUCCESS:
			processGlobalRequestSuccess(msg);
			return true;
		case SSH_MSG_CHANNEL_SUCCESS:
			processChannelRequestResponse(true, msg);
			return true;
		case SSH_MSG_CHANNEL_FAILURE:
			processChannelRequestResponse(false, msg);
			return true;
		default:
			return false;
		}

	}
	
	/**
	 * Process a global request success response.
	 */
	protected void processGlobalRequestSuccess(byte[] m) {
		
		ByteArrayReader msg = new ByteArrayReader(m);
		msg.skip(1);
		try {
			GlobalRequest request = outstandingRequests.removeFirst();
			if(Log.isDebugEnabled()) {
				Log.debug("Received SSH_MSG_GLOBAL_REQUEST_SUCCESS for " + request.getName());
			}
			if (msg.available() > 0) {
				byte[] tmp = new byte[msg.available()];
				try {
					msg.readFully(tmp);
					request.setData(tmp);
				} catch (IOException e) {
					Log.error("Unexpected error reading global request " + request.getName() + " response");
				}
	
			} else {
				request.setData(new byte[0]);
			}
			request.complete(true);
		} finally {
			msg.close();
		}
	}

	/**
	 * Process a global request failure
	 */
	protected void processGlobalRequestFailure(byte[] msg) {
		GlobalRequest request = outstandingRequests.removeFirst();
		if(Log.isDebugEnabled()) {
			Log.debug("Received SSH_MSG_GLOBAL_REQUEST_FAILURE for " + request.getName());
		}
		request.complete(false);
	}

	private void processChannelRequestResponse(boolean success, byte[] msg) {
		
		ByteArrayReader bar = new ByteArrayReader(msg);
		bar.skip(1);
		try {
			int channelid = (int) bar.readInt();

			
			ChannelNG<T> channel = getChannel(channelid);
			if(channel==null) {
				if(Log.isErrorEnabled()) {
					Log.error("Channel response received with invalid channel id {}", channelid);
				}
			} else {
				channel.processChannelRequestResponse(success);
			}
			
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} finally {
			bar.close();
		}
	}

	void processGlobalRequest(byte[] msg) throws IOException {
		
		ByteArrayReader bar = new ByteArrayReader(msg);
		
		try {
			bar.skip(1);

			String name = bar.readString();
			boolean wantreply = bar.read() != 0;
			boolean success = false;
			ByteArrayWriter response = new ByteArrayWriter();

			if(Log.isDebugEnabled()) {
				Log.debug("Received SSH_MSG_GLOBAL_REQUEST request="
						+ name + " wantReply=" + wantreply); 
			}
			
			if (name.equals("tcpip-forward")) {
				if(processTCPIPForward(bar, response)) {
					success = true;
				} 

			} else if (name.equals("cancel-tcpip-forward")) {
				if(processTCPIPCancel(bar, response)) {
					success = true;
				} 
			} else if (name.equals("ping@sshtools.com")) {
				/**
				 * Only for show; the remote side only cares if it gets a response
				 * not what the actual value is, but we are positive so send a success
				 * message rather than the default failure message.
				 */
				success = true;
			} else {

				@SuppressWarnings("unchecked")
				GlobalRequestHandler<T> handler 
					= (GlobalRequestHandler<T>) getContext().getGlobalRequestHandler(name);

				if (handler == null) {
					handler = globalRequestHandlers.get(name);
				}
				if (handler != null) {
					byte[] requestdata = new byte[bar.available()];
					bar.read(requestdata);
					GlobalRequest request = new GlobalRequest(name, con, requestdata);
					
					try {
						success = handler.processGlobalRequest(request, this, wantreply, response);
					} catch (GlobalRequestHandlerException e) {
					}
				}
			}

			if (wantreply) {
				if (success) {
					sendGlobalRequestSuccess(name, response.toByteArray());
				} else {
					sendGlobalRequestFailure(name);
				}
			}
		} finally {
			bar.close();
		}
	}

	protected abstract boolean processTCPIPCancel(ByteArrayReader bar, ByteArrayWriter msg) throws IOException;

	protected abstract boolean processTCPIPForward(ByteArrayReader bar, ByteArrayWriter response) throws IOException;
	
	void processChannelData(byte[] msg) throws IOException {
		ByteArrayReader bar = new ByteArrayReader(msg);

		try {
			int messageid = bar.read();
			int channelid = (int) bar.readInt();
			ChannelNG<T> channel = getChannel(channelid);

			if (channel == null) {
				if(Log.isErrorEnabled()) {
					Log.error("Channel data received with invalid channel id {}", channelid);
				}
			} else {
				try {
					if (messageid == SSH_MSG_CHANNEL_DATA) {
						int count = (int) bar.readInt();
						addTask(CHANNEL_DATA_IN, new ConnectionAwareTask(con) {
							protected  void doTask() throws Throwable {
								channel.processChannelData(ByteBuffer.wrap(bar.array(), bar.getPosition(), count));
							}
						});
						
					} else {
						int type = (int) bar.readInt();
						int count = (int) bar.readInt();
						addTask(CHANNEL_DATA_IN, new ConnectionAwareTask(con) {
							protected  void doTask() throws Throwable {
								channel.processExtendedData(type,
										ByteBuffer.wrap(bar.array(), bar.getPosition(), count));
							}
						});
						
					}
				} catch (IOException ex) {
					Log.error("Error processing channel data", ex);
				}
			}
		} finally {
			bar.close();
		}
	}

	void processChannelWindowAdjust(byte[] msg) throws IOException {
		ByteArrayReader bar = new ByteArrayReader(msg);
		bar.skip(1);
		try {
			// read message id and throw away. int messageid =
			int channelid = (int) bar.readInt();
			UnsignedInteger32 count = bar.readUINT32();

			ChannelNG<T> channel = getChannel(channelid);

			if (channel == null) {
				if(Log.isErrorEnabled()) {
					Log.error("Channel window adjust received with invalid channel id {}", channelid);
				}
			} else {
				if(Log.isDebugEnabled())
					Log.debug("Received SSH_MSG_CHANNEL_WINDOW_ADJUST channel="
							+ channelid + " remote="
							+ channel.remoteid + " adjust=" + count);
				channel.adjustWindow(count);
			}
		} finally {
			bar.close();
		}

	}

	void processChannelEOF(byte[] msg) throws IOException {
		ByteArrayReader bar = new ByteArrayReader(msg);
		bar.skip(1);
		try {
			// read message id and throw away. int messageid =
			int channelid = (int) bar.readInt();

			ChannelNG<T> channel = getChannel(channelid);

			if (channel == null) {
				if(Log.isErrorEnabled()) {
					Log.error("Channel EOF received with invalid channel id {}", channelid);
				}
			} else {

				addTask(CHANNEL_DATA_IN, new ConnectionAwareTask(con) {
					protected  void doTask() throws Throwable {
						if(Log.isDebugEnabled())
							Log.debug("Received SSH_MSG_CHANNEL_EOF channel="
									+ channelid + " remote="
									+ channel.remoteid);
						channel.processChannelEOF();
					}
				});
				

			}
		} finally {
			bar.close();
		}

	}

	ChannelNG<T> getChannel(int channelid) {
		return activeChannels.get(channelid);
	}

	void processChannelClose(byte[] msg) throws IOException {

		ByteArrayReader bar = new ByteArrayReader(msg);
		bar.skip(1);
		try {
			// read message id and throw away. int messageid =
			int channelid = (int) bar.readInt();

			ChannelNG<T> channel = getChannel(channelid);

			if (channel == null) {
				if(Log.isErrorEnabled()) {
					Log.error("Channel close received with invalid channel id {}", channelid);
				}
			} else {

				/**
				 * LDP - ensure channel is closed on the same queue as data processing
				 * to avoid data truncation.
				 */
				addTask(CHANNEL_DATA_IN, new ConnectionAwareTask(con) {
					protected  void doTask() throws Throwable {
						if(Log.isDebugEnabled()) {
							Log.debug("Received SSH_MSG_CHANNEL_CLOSE channel="
									+ channelid + " remote="
									+ channel.remoteid);
						}
						channel.processChannelClose();
					}
				});
				
			}
		} finally {
			bar.close();
		}
	}

	void processChannelOpenConfirmation(byte[] msg) throws IOException {
		ByteArrayReader bar = new ByteArrayReader(msg);
		bar.skip(1);
		try {

			int channelid = (int) bar.readInt();

			ChannelNG<T> channel = getChannel(channelid);

			if (channel == null) {
				if(Log.isErrorEnabled()) {
					Log.error("Channel confirmation received with invalid channel id {}", channelid);
				}
			} else {

				int remoteid = (int) bar.readInt();
				UnsignedInteger32 remotewindow = bar.readUINT32();
				int remotepacket = (int) bar.readInt();
				byte[] responsedata = null;
				if (bar.available() > 0) {
					responsedata = new byte[bar.available()];
					bar.read(responsedata);
				}
	
				if(Log.isDebugEnabled()) {
					Log.debug("Received SSH_MSG_CHANNEL_OPEN_CONFIRMATION channel=" + channelid + " remote="
							+ remoteid + " remotepacket=" + remotepacket + " remotewindow=" + remotewindow);
				}
				
				if(remotepacket < 4096) {
					transport.postMessage(new ChannelFailureMessage(remoteid,
							ChannelOpenException.ADMINISTRATIVIVELY_PROHIBITED,
							"Maximum remote packet size must be >= 4096 bytes"));
					return;	
				}
				
				synchronized (channel) {
					channel.confirmOpen(remoteid, remotewindow, remotepacket);
					// channel.notify();
				}
			}

		} finally {
			bar.close();
		}
	}

	void processChannelOpenFailure(byte[] msg) throws IOException {
		ByteArrayReader bar = new ByteArrayReader(msg);
		bar.skip(1);
		try {

			int channelid = (int) bar.readInt();

			ChannelNG<T> channel = getChannel(channelid);

			if (channel == null) {
				if(Log.isErrorEnabled()) {
					Log.error("Channel open failure received with invalid channel id {}", channelid);
				}
			} else {

				if(Log.isDebugEnabled()) {
					Log.debug("Received SSH_MSG_CHANNEL_OPEN_FAILURE channel="
							+ channelid);
				}
	
				synchronized (channel) {
					channel.fail();
					freeChannel(channel);
				}
			}
		} finally {
			bar.close();
		}

	}

	void processChannelOpen(byte[] msg) throws IOException {

		ByteArrayReader bar = new ByteArrayReader(msg);
		bar.skip(1);
		try {
		
			String channeltype = bar.readString();
			int remoteid = (int) bar.readInt();
			UnsignedInteger32 remotewindow = bar.readUINT32();
			int remotepacket = (int) bar.readInt();
			byte[] requestdata = null;
			if (bar.available() > 0) {
				requestdata = new byte[bar.available()];
				bar.read(requestdata);
			}

			if(Log.isDebugEnabled()) {
				Log.debug("Received SSH_MSG_CHANNEL_OPEN channeltype=" + channeltype + " remote="
						+ remoteid + " remotepacket=" + remotepacket + " window=" + remotewindow);
			}
			
			if(remotepacket < 4096) {
				transport.postMessage(new ChannelFailureMessage(remoteid,
						ChannelOpenException.ADMINISTRATIVIVELY_PROHIBITED,
						"Maximum remote packet size must be >= 4096 bytes"));
				return;	
			}

			ChannelNG<T> channel;
			
			try {
				channel = createChannel(channeltype, con);
			} catch (UnsupportedChannelException e) {
				
				transport.postMessage(new ChannelFailureMessage(remoteid,
						ChannelOpenException.UNKNOWN_CHANNEL_TYPE,
						"Unknown channel type " + channeltype));
				return;
			} catch (PermissionDeniedException e) {
				transport.postMessage(new ChannelFailureMessage(remoteid,
						ChannelOpenException.ADMINISTRATIVIVELY_PROHIBITED,
						"No permission for " + channeltype));
				return;
			} catch (ChannelOpenException e) {
				transport.postMessage(new ChannelFailureMessage(remoteid,
						e.getReason(),
						e.getMessage() == null ? "<null>" : e.getMessage()));
				return;
			}

			channel.init(this);

			int channelid = allocateChannel(channel);

			if (channelid > -1) {
				try {
					sendChannelOpenConfirmation(channel, channel.open(
							channelid, remoteid, remotepacket,
							remotewindow, requestdata));

					channel.onChannelOpen();

					return;
				} catch (ChannelOpenException ex) {
					transport.postMessage(new ChannelFailureMessage(
							remoteid, ex.getReason(), 
							ex.getMessage() == null ? "<null>" : ex.getMessage()));
				} catch (WriteOperationRequest ex) {
					// Just return as the channel has decided to
					// perform this asynchronously
				}
			} else {
				transport.postMessage(new ChannelFailureMessage(remoteid,
						ChannelOpenException.RESOURCE_SHORTAGE,
						"Maximum number of open channels exceeded"));
			}

		} finally {
			bar.close();
		}
	}

	protected abstract ChannelNG<T> createChannel(String channeltype, Connection<T> con) throws UnsupportedChannelException, PermissionDeniedException, ChannelOpenException;

	public void sendGlobalRequest(GlobalRequest request, boolean wantReply) {
		if(Log.isDebugEnabled()) {
			Log.debug("Sending SSH_MSG_GLOBAL_REQUEST request={} wantReply={}", request.getName(), String.valueOf(wantReply));
		}
		if(wantReply) {
			outstandingRequests.addLast(request);
		}
		transport.postMessage(new GlobalRequestMessage(request, wantReply));
	}

	class GlobalRequestMessage implements SshMessage {
		GlobalRequest request;
		byte[] name;
		boolean wantReply;
		
		GlobalRequestMessage(GlobalRequest request, boolean wantReply) {
			try {
				this.request = request;
				this.name = request.getName().getBytes(TransportProtocol.CHARSET_ENCODING);
				this.wantReply = wantReply;
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException("System does not support " + TransportProtocol.CHARSET_ENCODING);
			}
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {

			buf.put((byte) SSH_MSG_GLOBAL_REQUEST);
			buf.putInt(name.length);
			buf.put(name);
			buf.put((byte)(wantReply ? 1 : 0));
			if (request.getData() != null) {
				buf.put(request.getData());
			}

			return true;
		}
		
		/**
		 * The message has been sent.
		 */
		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled())
				Log.debug("Sent SSH_MSG_GLOBAL_REQUEST request="
						+ request.getName() + " wantReply=" + String.valueOf(wantReply));
		}

	}


	public int getQueueSize() {
		return transport.getQueueSizes();
	}

	public void sendChannelOpenConfirmation(ChannelNG<T> channel, byte[] responsedata) {
		transport.postMessage(new ChannelOpenConfirmationMessage(channel,
				responsedata));
		channel.confirmOpen();
	}

	public void sendChannelOpenFailure(ChannelNG<T> channel, int reason, String desc) {
		transport.postMessage(new ChannelFailureMessage(channel.getRemoteId(),
				reason, desc));
		freeChannel(channel);
	}

	void sendGlobalRequestSuccess(String name, byte[] responsedata) {
		transport.postMessage(new GlobalRequestSuccess(name, responsedata));
	}

	void sendGlobalRequestFailure(String name) {
		transport.postMessage(new GlobalRequestFailure(name));
	}

	void processChannelRequest(byte[] msg) throws IOException {

		ByteArrayReader bar = new ByteArrayReader(msg);
		bar.skip(1);
		try {
			
			int channelid = (int) bar.readInt();
			String requesttype = bar.readString();
			boolean wantreply = bar.read() != 0;
			byte[] requestdata = null;
			if (bar.available() > 0) {
				requestdata = new byte[bar.available()];
				bar.read(requestdata);
			}

			ChannelNG<T> channel = getChannel(channelid);

			if (channel != null) {

				if(Log.isDebugEnabled()) {
					Log.debug("Received SSH_MSG_CHANNEL_REQUEST '" + requesttype + "' channel="
							+ channelid + "  remote="
							+ channel.remoteid);
				}

				channel.onChannelRequest(requesttype, wantreply, requestdata);
			} else {
				if(Log.isErrorEnabled()) {
					Log.error("Channel request received with invalid channel id {}", channelid);
				}
			}

		} finally {
			bar.close();
		}
	}

	/**
	 * Get the connections {@link ConfigurationContext}.
	 * 
	 * @return SshContext
	 */
	public T getContext() {
		return transport.getSshContext();
	}

	/**
	 * Get the underlying transport. Use with Caution.
	 * 
	 * @return TransportProtocol
	 */
	public TransportProtocol<T> getTransport() {
		return transport;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.maverick.sshd.Service#start()
	 */
	public void start() {
		onStart();
	}

	protected abstract void onStart();
	
	class GlobalRequestFailure implements SshMessage {
		
		String name;
		public GlobalRequestFailure(String name) {
			this.name = name;
		}
		
		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put((byte) SSH_MSG_GLOBAL_REQUEST_FAILURE);
			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled())
				Log.debug("Sent SSH_MSG_GLOBAL_REQUEST_FAILURE request=" + name);
		}

	}

	class GlobalRequestSuccess implements SshMessage {

		byte[] responsedata;
		String name;
		GlobalRequestSuccess(String name, byte[] responsedata) {
			this.responsedata = responsedata;
			this.name = name;
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put((byte) SSH_MSG_GLOBAL_REQUEST_SUCCESS);
			if (responsedata != null) {
				buf.put(responsedata);
			}
			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled())
				Log.debug("Sent SSH_MSG_GLOBAL_REQUEST_SUCCESS request=" + name);
		}

	}

	class ChannelOpenMessage implements SshMessage {
		ChannelNG<T> channel;
		byte[] requestdata;

		ChannelOpenMessage(ChannelNG<T> channel, byte[] requestdata) {
			this.channel = channel;
			this.requestdata = requestdata;
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put((byte) SSH_MSG_CHANNEL_OPEN);
			buf.putInt(channel.getChannelType().length());
			buf.put(channel.getChannelType().getBytes());
			buf.putInt(channel.getLocalId());
			buf.put(ByteArrayWriter.encodeInt(channel.getLocalWindow()));
			buf.putInt(channel.getLocalPacket());

			if (requestdata != null) {
				buf.put(requestdata);
			}

			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled()) {
				Log.debug("Sent SSH_MSG_CHANNEL_OPEN channel="
						+ channel.getLocalId() + " channelType=" + channel.getChannelType());
			}
		}

	}

	class ChannelOpenConfirmationMessage implements SshMessage {
		ChannelNG<T> channel;
		byte[] responsedata;

		ChannelOpenConfirmationMessage(ChannelNG<T> channel, byte[] responsedata) {
			this.channel = channel;
			this.responsedata = responsedata;
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put((byte) SSH_MSG_CHANNEL_OPEN_CONFIRMATION);
			buf.putInt(channel.remoteid);
			buf.putInt(channel.getLocalId());
			buf.put(ByteArrayWriter.encodeInt(channel.getLocalWindow()));
			buf.putInt(channel.getLocalPacket());

			if (responsedata != null) {
				buf.put(responsedata);
			}

			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled())
				Log.debug("Sent SSH_MSG_CHANNEL_OPEN_CONFIRMATION channel="
						+ channel.channelid + " remote=" + channel.remoteid);
		}

	}

	class ChannelFailureMessage implements SshMessage {

		int remoteid;
		int reasoncode;
		String description;

		ChannelFailureMessage(int remoteid, int reasoncode, String description) {
			this.remoteid = remoteid;
			this.reasoncode = reasoncode;
			this.description = description;
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put((byte) SSH_MSG_CHANNEL_OPEN_FAILURE);
			buf.putInt(remoteid);
			buf.putInt(reasoncode);
			buf.putInt(description.length());
			buf.put(description.getBytes());
			buf.putInt(0);

			return true;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled())
				Log.debug("Sent SSH_MSG_CHANNEL_OPEN_FAILURE {} {} remote={}", description, reasoncode, remoteid);
		}

	}

	public String getUUID() {
		return getSessionIdentifier();
	}

	public int getIdleTimeoutSeconds() {
		return transport.getContext().getKeepAliveInterval();
	}
	
	public boolean idle() {

		for(ChannelNG<?> c : activeChannels.values()) {
			try {
				if(Log.isDebugEnabled()) {
					c.log();
				}
				if(c.getTimeout() > 0 && System.currentTimeMillis()-c.getLastActivity() > c.getTimeout()) {
					if(Log.isDebugEnabled()) {
						Log.debug("Closing idle channel channel={} remote={}", c.getLocalId(), c.getRemoteId());
					}
					c.close(true);
				}
			} catch (Throwable t) {
				Log.error("Error processing channel idle", t);
			}
		}

		if(getContext().getIdleConnectionTimeoutSeconds() == 0) {
			/**
			 * LDP - This mechanism will keep a connection open. Which will override an idle timeout
			 * when set, so we only use it when the idle timeout is zero.
			 */
			addTask(ExecutorOperationSupport.CALLBACKS, new ConnectionTaskWrapper(getConnection(), new Runnable() {
				public void run() {
					GlobalRequest global = new GlobalRequest(
							"ping@sshtools.com", 
							con, null);
					sendGlobalRequest(global, true);
					global.waitFor(30000L);
					if(!global.isDone()) {
						if(Log.isInfoEnabled()) {
							Log.error("Remote node is unresponsive");
						}
						getTransport().kill();
					}
				}
			}));
		}

		return false;
	}
	
	protected abstract boolean isClient();
	
	public Connection<T> getConnection() {
		return con;
	}
	
	public String getIdleLog() {
		return String.format("%d channels currently open", activeChannels.size());
	}
}
