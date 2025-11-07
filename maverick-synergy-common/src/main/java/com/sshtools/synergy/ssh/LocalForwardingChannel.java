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

import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.forwarding.ForwardingRequest.ForwardingRole;
import com.sshtools.common.forwarding.ForwardingRequest.ForwardingType;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.nio.ClientConnector;

/**
 * Implements a Local forwarding channel for use with forwarding sockets from
 * the client machine through the server to some endpoint reachable from the
 * server machine.
 */
public class LocalForwardingChannel<T extends SshContext>
		extends AbstractLocalSocketForwardingChannel<T>
		implements ClientConnector, TCPForwardingChannel {

	/**Tunnel endpoint hostname*/
    protected String hostToConnect;
    /**Tunnel endpoint port number*/
    protected int portToConnect;
    /**Tunnel startpoint hostname*/
    protected String originatingHost;
    /**Tunnel startpoint port number*/
    protected int originatingPort;

	public LocalForwardingChannel(String channelType, SshConnection con) {
		super(channelType, con);
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
	public LocalForwardingChannel(String channelType, SshConnection con, String hostToConnect, int portToConnect,
			SocketChannel socketChannel) {
		super(channelType,  con);
		this.socketChannel = socketChannel;
		this.hostToConnect = hostToConnect;
		this.portToConnect = portToConnect;
	}
	/**
	 * Create the forwarding channel.
	 *
	 * @return byte[]
	 */
	@Override
	protected byte[] createChannel() throws IOException {


		ByteArrayWriter baw = new ByteArrayWriter();

		try {
			baw.writeString(hostToConnect);
			baw.writeInt(portToConnect);
			baw.writeString(originatingHost = ((InetSocketAddress) socketChannel
					.socket().getRemoteSocketAddress()).getAddress()
					.getHostAddress());
			baw.writeInt(originatingPort = ((InetSocketAddress) socketChannel
					.socket().getRemoteSocketAddress()).getPort());

			return baw.toByteArray();

		} finally {
			baw.close();
		}
	}

	@Override
	protected void readForwarding(ByteArrayReader bar) throws IOException {
		hostToConnect = bar.readString();
		portToConnect = (int) bar.readInt();
		originatingHost = bar.readString();
		originatingPort = (int) bar.readInt();
	}

	@Override
	protected SocketAddress createSocketAddress() {
		return new InetSocketAddress(hostToConnect,
				portToConnect);
	}

	@Override
	protected SocketChannel createSocketChannel() throws IOException {
		var socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setTcpNoDelay(true);
		return socketChannel;
	}

	@Override
	protected boolean checkPermissions() {
		return getContext().getForwardingPolicy().validate(
				getConnectionProtocol().getTransport().getConnection(),
				ForwardingRole.CONNECT,
				ForwardingType.LOCAL, ForwardingRequest.ofTcpDestination(
					hostToConnect,
					portToConnect)
				);
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

	@Override
	protected String targetToConnect() {
		return hostToConnect + ":" + portToConnect;
	}


}
