package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import com.sshtools.common.forwarding.ForwardingHandle;
import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.forwarding.ForwardingRequest.ForwardingRole;
import com.sshtools.common.forwarding.ForwardingRequest.ForwardingType;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.IOUtils;
import com.sshtools.synergy.nio.ClientConnector;

/**
 * Implements a Remote forwarding channel for use with forwarding sockets from
 * the server machine through the client to some endpoint reachable from the
 * client machine.
 * <p>
 * This base class is used by both client and server specialised implementations.
 */
public class RemoteForwardingChannel<T extends SshContext> extends AbstractRemoteSocketForwardingChannel<T> implements ClientConnector, TCPForwardingChannel {

	/**Tunnel endpoint hostname*/
    protected String hostToConnect;
    /**Tunnel endpoint port number*/
    protected int portToConnect;
    /**Tunnel startpoint hostname*/
    protected String originatingHost;
    /**Tunnel startpoint port number*/
    protected int originatingPort;

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

    @Override
	public String getHost() {
        return hostToConnect;
    }

    @Override
	public int getPort() {
        return portToConnect;
    }

    @Override
	public String getOriginatingHost() {
        return originatingHost;
    }

    @Override
	public int getOriginatingPort() {
        return originatingPort;
    }

	/**
	 * Creates the end of the channel open message string address that was
	 * connected uint32 port that was connected string originator IP address
	 * uint32 originator port
	 *
	 * @return byte[], the end of the channelopenmessage
	 * @throws IOException
	 */
	@Override
	protected byte[] createChannel() throws IOException {

		boolean success = true;

		if (!getContext().getForwardingPolicy().validate(
				getConnectionProtocol().getTransport().getConnection(),
				ForwardingRole.CONNECT,
				ForwardingType.REMOTE, ForwardingRequest.ofTcpDestination(hostToConnect, portToConnect))) {
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
				if(Log.isTraceEnabled()) {
					Log.trace("Failed to close socket channel", t);
				}
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

	@Override
	protected byte[] openChannel(byte[] requestdata)
			throws WriteOperationRequest, ChannelOpenException {

		ByteArrayReader bar = new ByteArrayReader(requestdata);
		try {

			String addressToBind = bar.readString();
			int portToBind = (int) bar.readInt();
			originatingHost = bar.readString();
			originatingPort = (int) bar.readInt();

			@SuppressWarnings("unchecked")
			ForwardingManager<T> forwardingManager = (ForwardingManager<T>) getContext().getForwardingManager();
			ForwardingHandle remoteForward = forwardingManager.getRemoteBinds(getConnectionProtocol()).
					stream().
					peek(hndl -> {
						if(Log.isDebugEnabled()) {
							Log.debug("Matching forward:  {} against {}:{}",
									hndl.request().bindName(),
									hndl.boundPort().orElse(0), portToBind);
						}
					}).
					filter(hndl ->
						hndl.request().bindAddress().equals(addressToBind) &&
						hndl.boundPort().orElse(0) == portToBind
					).
					findFirst().orElseThrow(() -> new ChannelOpenException("Remote forwarding not available",
						ChannelOpenException.ADMINISTRATIVIVELY_PROHIBITED));

			ForwardingRequest request = remoteForward.request();

			hostToConnect = request.destinationAddress();
			portToConnect = request.destinationPort();

			boolean success = getContext().getForwardingPolicy().validate(
					getConnectionProtocol().getTransport().getConnection(),
					ForwardingRole.CONNECT,
					ForwardingType.REMOTE, ForwardingRequest.ofTcpDestination(
						hostToConnect,
						portToConnect)
					);

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
			createSocketChannel();

			if (socketChannel.connect(createSocketAddress())) {
				if(Log.isInfoEnabled()) {
					if(Log.isInfoEnabled()) {
						Log.info("Remote forwarding socket to {}:{} has connected [synchronously] channel={} remote={}",
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

	@Override
	protected SocketAddress createSocketAddress() {
		return new InetSocketAddress(hostToConnect,
				portToConnect);
	}

	@Override
	protected void createSocketChannel() throws IOException {
		socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setTcpNoDelay(true);
	}

	@Override
	protected String targetToConnect() {
		return hostToConnect + ":" + portToConnect;
	}

}
