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
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.ClientConnector;
import com.sshtools.common.nio.ProtocolEngine;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.IOUtils;

/**
 * Implements a Remote forwarding channel for use with forwarding sockets from
 * the server machine through the client to some endpoint reachable from the
 * client machine.
 */
public class RemoteForwardingChannel<T extends SshContext> extends SocketForwardingChannel<T> implements ClientConnector {

	boolean hasConnected = false;
	
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
	public RemoteForwardingChannel(SshConnection con, String addressToBind, int portToBind, SocketChannel socketChannel) {
		super(REMOTE_FORWARDING_CHANNEL_TYPE, con);
		this.socketChannel = socketChannel;
		this.hostToConnect = addressToBind;
		this.portToConnect = portToBind;
	}

	protected RemoteForwardingChannel(SshConnection con) {
		super(REMOTE_FORWARDING_CHANNEL_TYPE, con);
	}

	/**
	 * 
	 * @param name
	 * @param addressToBind
	 * @param portToBind
	 * @param socketChannel
	 */
	public RemoteForwardingChannel(String name, SshConnection con, String addressToBind, int portToBind, SocketChannel socketChannel, T context) {
		super(name, con);
		this.socketChannel = socketChannel;
		this.hostToConnect = addressToBind;
		this.portToConnect = portToBind;
	}

	/**
	 * Creates the end of the channel open message string address that was
	 * connected uint32 port that was connected string originator IP address
	 * uint32 originator port
	 * 
	 * @return byte[], the end of the channelopenmessage
	 * @throws IOException
	 */
	protected byte[] createChannel() throws IOException {

		boolean success = true;

		if (!getContext().getForwardingPolicy().checkHostPermitted(
				getConnectionProtocol().getTransport().getConnection(), hostToConnect, portToConnect)) {
			success = false;

			if(Log.isDebugEnabled()) {
				Log.debug("Forwarding policy has " + (success ? "authorized" : "denied") + " "
						+ connection.getUsername() + (success ? " to open" : " from opening")
						+ " a local forwarding channel to " + hostToConnect + ":" + portToConnect);
			}
		}

		if (!success) {

			try {
				socketChannel.close();
			} catch (Throwable t) {
				if(Log.isTraceEnabled())
					Log.trace("Failed to close socket channel", t);
			}

			throw new IOException("Cannot create channel because access has been denied by forwarding policy");
		}

		ByteArrayWriter baw = new ByteArrayWriter();

		try {
			if (!getChannelType().equals(X11_FORWARDING_CHANNEL_TYPE)) {
				baw.writeString(hostToConnect);
				baw.writeInt(portToConnect);
			}
			baw.writeString(originatingHost = ((InetSocketAddress) socketChannel.socket().getRemoteSocketAddress())
					.getAddress().getHostAddress());
			baw.writeInt(
					originatingPort = ((InetSocketAddress) socketChannel.socket().getRemoteSocketAddress()).getPort());

			return baw.toByteArray();

		} finally {
			baw.close();
		}
	}

	protected void onRegistrationComplete() {
		if(Log.isTraceEnabled())
			Log.trace("Registration Complete channel=%d", getLocalId());
	}

	protected void onChannelOpenConfirmation() {
		// Register the handler
		try {
			getContext().getEngine().registerHandler(this, socketChannel);
		} catch (IOException ex) {
			if(Log.isTraceEnabled())
				Log.trace("Failed to register channel with a selector", ex);
		}
	}

	protected byte[] openChannel(byte[] requestdata)
			throws WriteOperationRequest, ChannelOpenException {
		
		ByteArrayReader bar = new ByteArrayReader(requestdata);
		try {

			String addressToBind = bar.readString();
			int portToBind = (int) bar.readInt();
			originatingHost = bar.readString();
			originatingPort = (int) bar.readInt();

			@SuppressWarnings("unchecked")
			Map<String,RemoteForward> remoteForwards = (Map<String,RemoteForward>) 
					getConnectionProtocol().getConnection().getProperty("remoteForwards");
			
			RemoteForward remoteForward = remoteForwards.get(addressToBind + ":" + portToBind);

			if(remoteForward==null) {
				throw new ChannelOpenException("Remote forwarding not available",
						ChannelOpenException.ADMINISTRATIVIVELY_PROHIBITED);
			}
			hostToConnect = remoteForward.getHostToConnect();
			portToConnect = remoteForward.getPortToConnect();
			
			boolean success = getContext().getForwardingPolicy().checkHostPermitted(
					getConnectionProtocol().getTransport().getConnection(), hostToConnect,
					portToConnect);
			
			if(Log.isDebugEnabled()) {
				Log.debug("Forwarding policy has "
						+ (success ? "authorized" : "denied") + " "
						+ connection.getUsername()
						+ (success ? " to open" : " from opening")
						+ " a " + getChannelType() + " forwarding channel to " + hostToConnect
						+ ":" + portToConnect);
			}

			if (!success) {
				throw new ChannelOpenException("User does not have permission",
						ChannelOpenException.ADMINISTRATIVIVELY_PROHIBITED);
			}


			
			// Create a non-blocking socket channel
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.socket().setTcpNoDelay(true);

			if (socketChannel.connect(new InetSocketAddress(hostToConnect,
					portToConnect))) {
				if(Log.isInfoEnabled()) {
					if(Log.isInfoEnabled()) {
						Log.info("Remote forwarding socket to %s:%d has connected [synchronously] channel=%d remote=%d",
								hostToConnect,
								portToConnect,
								getLocalId(),
								getRemoteId());
					}
				}
				hasConnected = true;
				return null;
			}
			
			// Register the connector and we will confirm once weve connected
			connection.getContext().getEngine().registerConnector(this, socketChannel);


		} catch (Throwable ex) {
			IOUtils.closeStream(socketChannel);
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

	/**
	 * Called when the forwarded socket has been connected.
	 * 
	 * @param key
	 *            SelectionKey
	 * @return boolean
	 */
	public synchronized boolean finishConnect(SelectionKey key) {

		if (socketChannel == null)
			return true;

		if(hasConnected) {
			if(Log.isWarnEnabled()) {
				Log.warn("Duplicate finishConnect call to %s:%d channel=%d", hostToConnect, 
					portToConnect, getLocalId());
			}
			return true;
		}
		
		hasConnected = true;
		
		try {
			while (!socketChannel.finishConnect()) {
				// Wait for the connection to complete
			}
			if(Log.isInfoEnabled()) {
				if(Log.isInfoEnabled()) {
					Log.info("Remote forwarding socket to %s:%d has connected [asynchronously] channel=%d remote=%d",
							hostToConnect,
							portToConnect,
							getLocalId(),
							getRemoteId());
				}
			}

			connection.sendChannelOpenConfirmation(this, null);

		} catch (IOException ex) {
			if(Log.isInfoEnabled()) {
				Log.info("Remote forwarding socket to %s:%d has failed \"%s\" channel=%d remote=%d",
							hostToConnect,
								portToConnect, 
								ex.getMessage(),
								getLocalId(),
								getRemoteId());
			}
			connection.sendChannelOpenFailure(this,
					ChannelOpenException.CONNECT_FAILED, "Connection failed.");
		}

		return true;
	}
	/**
	 * Either nothing was listening on the clients end of the tunnel, or the
	 * connection was rejected. Now we close the connection from the server to
	 * the start of the tunnel.
	 */
	protected void onChannelOpenFailure() {
		try {
			socketChannel.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void initialize(ProtocolEngine engine, SshEngine daemon, SelectableChannel channel) {

	}

}
