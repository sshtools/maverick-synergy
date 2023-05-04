/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.synergy.jdk16;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.RequestFuture;
import com.sshtools.common.ssh.RequestFutureListener;
import com.sshtools.common.util.IOUtils;
import com.sshtools.synergy.nio.ListeningInterface;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.SocketListeningForwardingChannelFactoryImpl;
import com.sshtools.synergy.ssh.SshContext;

public abstract class UnixDomainSocketForwardingChannelFactory<C extends SshContext>  
	extends SocketListeningForwardingChannelFactoryImpl<C> {


	@SuppressWarnings("unchecked")
	public int bindInterface(String addressToBind, int portToBind, ConnectionProtocol<?> connection, String channelType)
			throws IOException {

		this.addressToBind = addressToBind;
		this.portToBind = portToBind;
		this.connection = (ConnectionProtocol<C>) connection;
		this.channelType = channelType;

		addr = UnixDomainSocketAddress.of(addressToBind);

		this.socketChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);

		try {
			socketChannel.configureBlocking(false);
			if (connection.getContext().getReceiveBufferSize() > 0) {
				socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, connection.getContext().getReceiveBufferSize());
			}
			socketChannel.bind(addr, connection.getContext().getMaximumSocketsBacklogPerRemotelyForwardedConnection());

			connection.getContext().getEngine().registerAcceptor(this, socketChannel);

			return 0;

		} catch (IOException e) {
			IOUtils.closeStream(socketChannel);
			throw e;
		}
	}

	public boolean finishAccept(SelectionKey key, ListeningInterface li) {
		try {
			var sc = socketChannel.accept();

			if (sc != null) {

				if (Log.isDebugEnabled()) {
					Log.debug(channelType + " forwarding socket accepted ");
				}
				sc.configureBlocking(false);
				if (connection.getContext().getReceiveBufferSize() > 0) {
					socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, connection.getContext().getReceiveBufferSize());
				}
				if (connection.getContext().getSendBufferSize() > 0) {
					socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, connection.getContext().getSendBufferSize());
				}

				var channel = createChannel(channelType, connection.getTransport().getConnection(),
						addressToBind, portToBind, sc, connection.getContext());

				channel.addEventListener(activeRemoteForwardings);

				channel.getOpenFuture().addFutureListener(new RequestFutureListener() {
					public void complete(RequestFuture future) {

						if (!future.isSuccess()) {
							if (Log.isDebugEnabled()) {
								Log.debug("Channel could not be opened");
							}
							try {
								sc.close();
							} catch (IOException ex) {
							}
						}
					}
				});

				connection.openChannel(channel);

			} else {
				if (Log.isDebugEnabled()) {
					Log.debug("FORWARDING accept event fired but no socket was accepted");
				}
			}
		} catch (IOException ex) {
			if (Log.isDebugEnabled()) {
				Log.debug("Accept operation failed on " + addressToBind + ":" + portToBind, ex);
			}
		}

		return !socketChannel.isOpen();
	}
}
