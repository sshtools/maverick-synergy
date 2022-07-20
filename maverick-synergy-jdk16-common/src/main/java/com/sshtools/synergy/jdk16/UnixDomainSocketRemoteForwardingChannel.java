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
package com.sshtools.synergy.jdk16;

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
