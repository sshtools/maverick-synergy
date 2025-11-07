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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import com.sshtools.common.forwarding.ForwardingHandle;
import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.logger.Log;

/**
 * This class implements the standard socket based forwarding for the SSHD.
 */
public abstract class TCPSocketListeningForwardingChannelFactoryImpl<T extends SshContext>
      extends SocketListeningForwardingChannelFactoryImpl<T, InetSocketAddress> {

	@Override
	protected InetSocketAddress createAddress(ForwardingRequest request) {
    	return new InetSocketAddress(request.bindAddress(), request.bindPort());
    }
    
	@Deprecated(forRemoval = true, since = "3.2.0")
	public int bindInterface(String addressToBind, int portToBind, ConnectionProtocol<?> connection, String channelType) throws IOException {
        return bindInterface(ForwardingRequest.ofTcpBind(addressToBind, portToBind), connection).boundPort().get();
    }

	@Override
	protected void onAccept(final SocketChannel sc) throws SocketException {
		Socket socket = sc.socket();
		if(Log.isDebugEnabled()) { 
			InetSocketAddress remoteAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
			Log.debug(channelType + " TCP forwarding socket accepted from "
		          + remoteAddress.getAddress().getHostAddress()
		          + "/"
		          + remoteAddress.getAddress().getHostAddress()
		          + ":"
		          + remoteAddress.getPort());
		}
		T context = connection.getContext();
		if(context.getReceiveBufferSize() > 0) {
			socket.setReceiveBufferSize(context.getReceiveBufferSize());
		}
		if(context.getSendBufferSize() > 0) {
			socket.setSendBufferSize(context.getSendBufferSize());
		}
		socket.setKeepAlive(context.getSocketOptionKeepAlive());
		socket.setTcpNoDelay(context.getSocketOptionTcpNoDelay());
	}

	@Override
	protected ServerSocketChannel createSocketChannel() throws IOException {
		ServerSocketChannel socketChannel = ServerSocketChannel.open();
        ServerSocket socket = socketChannel.socket();
		socket.setReuseAddress(true);
        if(connection.getContext().getReceiveBufferSize() > 0) {
        	socket.setReceiveBufferSize(
        			connection.getContext().getReceiveBufferSize());
        }
		return socketChannel;
	}

	@Override
	protected ForwardingHandle createHandle() {
		 int boundPort = socketChannel.socket().getLocalPort();
	        
        return new ForwardingHandle() {

			@Override					
			public String toString() {
				return "{" + type().name() + "} : " + request + " @ " + boundPort().orElse(0);
			}

			@Override
			public void close(boolean killActiveTunnels) throws IOException {
				stopListening(killActiveTunnels);
			}

			@Override
			public ForwardingRequest.ForwardingType type() {
				return ForwardingRequest.ForwardingType.LOCAL;
			}

			@Override
			public ForwardingRequest request() {
				return request;
			}

			@Override
			public Optional<Integer> boundPort() {
				return Optional.of(boundPort);
			}

			@Override
			public Optional<String> boundPath() {
				return Optional.empty();
			}
        	
        };
	}
}
