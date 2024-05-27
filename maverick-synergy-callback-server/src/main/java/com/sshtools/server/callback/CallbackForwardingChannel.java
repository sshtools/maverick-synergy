package com.sshtools.server.callback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

import com.sshtools.client.SshClientContext;
import com.sshtools.common.forwarding.ForwardingPolicy;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.ExecutorOperationQueues;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.ForwardingChannel;
import com.sshtools.synergy.ssh.LocalForwardingChannel;
import com.sshtools.synergy.ssh.SshContext;

public class CallbackForwardingChannel<T extends SshContext> extends ForwardingChannel<T> {

	static final String DIRECT_STREAM_LOCAL_CHANNEL = "direct-streamlocal@openssh.com";
	CallbackForwardingChannel<?> channel;
	SshConnection callbackClient;
	final static Integer CHANNEL_QUEUE = ExecutorOperationQueues.generateUniqueQueue("callbackDataQueue");
	
	public CallbackForwardingChannel(Context ctx, SshConnection callbackClient) {
		this(LocalForwardingChannel.LOCAL_FORWARDING_CHANNEL_TYPE, ctx, callbackClient);
		this.callbackClient = callbackClient;
	}
	
	public CallbackForwardingChannel(String channel, Context ctx, SshConnection callbackClient) {
		super(channel, 
				ctx.getPolicy(ForwardingPolicy.class).getForwardingMaxPacketSize(),
				ctx.getPolicy(ForwardingPolicy.class).getForwardingMaxWindowSize(),
				ctx.getPolicy(ForwardingPolicy.class).getForwardingMaxWindowSize(),
				ctx.getPolicy(ForwardingPolicy.class).getForwardingMinWindowSize(),
				true);
		this.callbackClient = callbackClient;
	}
	
	public CallbackForwardingChannel(Context ctx, SshConnection callbackClient, String hostToConnect, int portToConnect) {
		super(LocalForwardingChannel.LOCAL_FORWARDING_CHANNEL_TYPE, 
				ctx.getPolicy(ForwardingPolicy.class).getForwardingMaxPacketSize(),
				ctx.getPolicy(ForwardingPolicy.class).getForwardingMaxWindowSize(),
				ctx.getPolicy(ForwardingPolicy.class).getForwardingMaxWindowSize(),
				ctx.getPolicy(ForwardingPolicy.class).getForwardingMinWindowSize(),
				true);
		this.callbackClient = callbackClient;
		this.hostToConnect = hostToConnect;
		this.portToConnect = portToConnect;
	}

	public void setBoundChannel(CallbackForwardingChannel<?> channel) {
		this.channel = channel;
	}
	
	/**
	 * Constructs a forwarding channel of the type "forwarded-tcpip"
	 * 
	 * @param addressToBind
	 *            String
	 * @param portToBind
	 *            int
	 * @param socketChannel
	 *            SocketChannel
	 */
	public CallbackForwardingChannel(String channelType, SshConnection con, String hostToConnect, int portToConnect) {
		super(channelType, con.getContext().getPolicy(ForwardingPolicy.class).getForwardingMaxPacketSize(),
				con.getContext().getPolicy(ForwardingPolicy.class).getForwardingMaxWindowSize(),
				con.getContext().getPolicy(ForwardingPolicy.class).getForwardingMaxWindowSize(),
				con.getContext().getPolicy(ForwardingPolicy.class).getForwardingMinWindowSize(),
				true);
		this.hostToConnect = hostToConnect;
		this.portToConnect = portToConnect;
	}
	
	/**
	 * Constructs a forwarding channel of the type "forwarded-tcpip"
	 * 
	 * @param addressToBind
	 *            String
	 * @param portToBind
	 *            int
	 * @param socketChannel
	 *            SocketChannel
	 */
	public CallbackForwardingChannel(String channelType, Context ctx, SshConnection con, String hostToConnect, int portToConnect) {
		super(channelType, con.getContext().getPolicy(ForwardingPolicy.class).getForwardingMaxPacketSize(),
				ctx.getPolicy(ForwardingPolicy.class).getForwardingMaxWindowSize(),
				ctx.getPolicy(ForwardingPolicy.class).getForwardingMaxWindowSize(),
				ctx.getPolicy(ForwardingPolicy.class).getForwardingMinWindowSize(),
				true);
		this.hostToConnect = hostToConnect;
		this.portToConnect = portToConnect;
	}
	
	/**
	 * Create the forwarding channel.
	 * 
	 * @return byte[]
	 */
	protected byte[] createChannel() throws IOException {
		if(getChannelType().equals(DIRECT_STREAM_LOCAL_CHANNEL)) {
			try(var baw = new ByteArrayWriter()) {
				baw.writeString(hostToConnect);
				baw.writeString(""); // Reserved
				baw.writeInt(0); // Reserved
				return baw.toByteArray();
	
			}
		}

		try(var baw = new ByteArrayWriter()) {
			baw.writeString(hostToConnect);
			baw.writeInt(portToConnect);
			baw.writeString(originatingHost = con.getRemoteIPAddress());
			baw.writeInt(originatingPort = con.getRemotePort());

			return baw.toByteArray();

		} 
	}

	/**
	 * Open a forwarding channel.
	 *
	 * @param requestdata
	 *            byte[]
	 * @return byte[]
	 * @throws WriteOperationRequest
	 * @throws ChannelOpenException
	 */
	protected byte[] openChannel(byte[] requestdata)
			throws WriteOperationRequest, ChannelOpenException {

		ByteArrayReader bar = new ByteArrayReader(requestdata);
		try {

			if(getChannelType().equals(DIRECT_STREAM_LOCAL_CHANNEL)) {
				hostToConnect = bar.readString(); // socket path
				bar.readString(); // reserved
				bar.readInt(); // reserved
			}
			else {
				hostToConnect = bar.readString();
				portToConnect = (int) bar.readInt();
				originatingHost = bar.readString();
				originatingPort = (int) bar.readInt();
			}

			boolean success = checkPermissions();

			if(Log.isDebugEnabled()) {
				Log.debug("Forwarding policy has "
						+ (success ? "authorized" : "denied") + " "
						+ connection.getUsername()
						+ (success ? " to open" : " from opening")
						+ " a " + getChannelType() + " callback forwarding channel to " + hostToConnect
						+ ":" + portToConnect);
			}

			if (!success) {
				throw new ChannelOpenException("User does not have permission",
						ChannelOpenException.ADMINISTRATIVIVELY_PROHIBITED);
			}

			if(Objects.isNull(callbackClient)) {
				throw new ChannelOpenException(
						String.format("Callback client %s is not connected", hostToConnect),
						ChannelOpenException.CONNECT_FAILED);
			}
			
			con.addTask(new ConnectionAwareTask(con) {

				@Override
				protected void doTask() throws Throwable {
					
				    channel = new CallbackForwardingChannel<SshClientContext>(getChannelType(), 
								connection.getContext(), callbackClient, hostToConnect, portToConnect);
					
				    channel.setBoundChannel(CallbackForwardingChannel.this);
					callbackClient.openChannel(channel);
					
					channel.getOpenFuture().waitFor(30000L);
					
					if(channel.getOpenFuture().isSuccess()) {
						connection.sendChannelOpenConfirmation(
								CallbackForwardingChannel.this, null);
					} else {
						connection.sendChannelOpenFailure(CallbackForwardingChannel.this, 
								ChannelOpenException.CONNECT_FAILED, 
								   "Callback client failed to complete channel open within timeout period");
					}
				}
				
			});
			
		} catch (Throwable ex) {
			throw new ChannelOpenException(
					ex.getMessage(),
					ChannelOpenException.CONNECT_FAILED);
		} finally {
			bar.close();
		}

		// Throw an WriteOperationRequest so that we can perform the
		// channel open confirmation or failure when the socket has
		// connected
		throw new WriteOperationRequest();
	}

	protected boolean checkPermissions() {
		return getContext().getForwardingPolicy().checkHostPermitted(
				getConnectionProtocol().getTransport().getConnection(), hostToConnect,
				portToConnect);
	}

	/**
	 * Called when the forwarded sockets selector has been registered with a
	 * {@link com,maverick.nio.SelectorThread}.
	 */
	protected synchronized void onRegistrationComplete() {
		// Now do nothing, connect is called only if it returns false above
		// and that means the connect procedure is already underway.
		if(Log.isDebugEnabled()) {
			Log.debug("Registration Complete");
		}
	}

	/**
	 * Called when the channel has been confirmed as open.
	 */
	protected synchronized void onChannelOpenConfirmation() {
		
	}
	
	@Override
	protected void onChannelData(ByteBuffer data) {
		con.addTask(CHANNEL_QUEUE, new ConnectionAwareTask(con) {

			@Override
			protected void doTask() throws Throwable {
				channel.sendChannelDataAndBlock(data);
				evaluateWindowSpace();
			}
		});
	}
	
	protected void onChannelOpenFailure() {

	}

	@Override
	protected void onChannelFree() {
	
	}

	@Override
	protected void onChannelClosed() {
		con.addTask(CHANNEL_QUEUE, new ConnectionAwareTask(con) {

			@Override
			protected void doTask() throws Throwable {
				channel.close();
			}
		});
	}

	@Override
	protected void onChannelOpen() {

	}

	@Override
	protected void onChannelClosing() {
		
	}

	@Override
	protected void onChannelRequest(String type, boolean wantreply, byte[] requestdata) {

	}

	@Override
	protected void onRemoteEOF() {
		channel.sendEOF();
	}

	@Override
	protected void onLocalEOF() {
		
	}


}
