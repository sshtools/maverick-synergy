package com.sshtools.synergy.jdk16;

/*-
 * #%L
 * Common code for Unix Domain Socket support
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
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;

import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.IOUtils;
import com.sshtools.synergy.ssh.RemoteForward;
import com.sshtools.synergy.ssh.RemoteForwardingChannel;
import com.sshtools.synergy.ssh.SshContext;

public class UnixDomainSocketRemoteForwardingChannel<T extends SshContext> extends RemoteForwardingChannel<T> {

	public UnixDomainSocketRemoteForwardingChannel(String name, SshConnection con, String addressToBind, int portToBind,
			SocketChannel socketChannel, T context) {
		super(name, con, addressToBind, portToBind, socketChannel, context);
	}


	protected byte[] openChannel(byte[] requestdata)
			throws WriteOperationRequest, ChannelOpenException {
		
		ByteArrayReader bar = new ByteArrayReader(requestdata);
		try {

			String pathToBind = bar.readString();
			bar.readString(); // reserved

			@SuppressWarnings("unchecked")
			Map<String,RemoteForward> remoteForwards = (Map<String,RemoteForward>) 
					getConnectionProtocol().getConnection().getProperty("remoteForwards");
			
			RemoteForward remoteForward = remoteForwards.get(pathToBind);

			if(remoteForward==null) {
				throw new ChannelOpenException("Remote forwarding not available",
						ChannelOpenException.ADMINISTRATIVIVELY_PROHIBITED);
			}
			hostToConnect = remoteForward.getHostToConnect();
			
			boolean success = getContext().getForwardingPolicy().checkHostPermitted(
					getConnectionProtocol().getTransport().getConnection(), hostToConnect,
					portToConnect);
			
			if(Log.isDebugEnabled()) {
				Log.debug("Forwarding policy has "
						+ (success ? "authorized" : "denied") + " "
						+ connection.getUsername()
						+ (success ? " to open" : " from opening")
						+ " a " + getChannelType() + " forwarding channel to " + hostToConnect);
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
	protected byte[] createChannel() throws IOException {
		boolean success = true;

		if (!getContext().getForwardingPolicy().checkHostPermitted(
				getConnectionProtocol().getTransport().getConnection(), hostToConnect, portToConnect)) {
			success = false;

			if (Log.isDebugEnabled()) {
				Log.debug("Forwarding policy has " + (success ? "authorized" : "denied") + " "
						+ connection.getUsername() + (success ? " to open" : " from opening")
						+ " a local domain socket forwarding channel to " + hostToConnect);
			}
		}

		if (!success) {

			try {
				socketChannel.close();
			} catch (Throwable t) {
				if (Log.isTraceEnabled())
					Log.trace("Failed to close socket channel", t);
			}

			throw new IOException("Cannot create channel because access has been denied by forwarding policy");
		}

		ByteArrayWriter baw = new ByteArrayWriter();

		try {
			baw.writeString(hostToConnect);
			baw.writeString(""); // reserved
			return baw.toByteArray();

		} finally {
			baw.close();
		}
	}

	@Override
	protected SocketAddress createSocketAddress() {
		return UnixDomainSocketAddress.of(hostToConnect);
	}

	@Override
	protected void createSocketChannel() throws IOException {
		socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
		socketChannel.configureBlocking(false);
	}
}
