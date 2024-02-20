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
