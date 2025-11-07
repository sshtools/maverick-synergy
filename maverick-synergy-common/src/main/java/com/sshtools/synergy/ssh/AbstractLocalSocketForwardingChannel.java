package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.IOUtils;
import com.sshtools.synergy.nio.ClientConnector;
import com.sshtools.synergy.nio.ProtocolEngine;
import com.sshtools.synergy.nio.SocketHandler;
import com.sshtools.synergy.nio.SshEngine;

public abstract class AbstractLocalSocketForwardingChannel<T extends SshContext> extends SocketForwardingChannel<T>
		implements SocketHandler, ClientConnector {

	boolean hasConnected = false;

	public AbstractLocalSocketForwardingChannel(String channeltype, SshConnection con) {
		super(channeltype, con);
	}

	/**
	 * Called when the forwarded sockets selector has been registered with a
	 * {@link com,maverick.nio.SelectorThread}.
	 */
	@Override
	protected synchronized void onRegistrationComplete() {
		// Now do nothing, connect is called only if it returns false above
		// and that means the connect procedure is already underway.
		if(Log.isDebugEnabled()) {
			Log.debug("Registration Complete");
		}
	}

	protected void onConnectionError(IOException ex) {
	}

	protected void onConnectionComplete() {
	}

	/**
	 * Called when the channel has been confirmed as open.
	 */
	@Override
	protected synchronized void onChannelOpenConfirmation() {

		try {
			connection.getContext().getEngine()
					.registerHandler(this, socketChannel);
		} catch (IOException ex) {
			if(Log.isDebugEnabled()) {
				Log.debug(
						"Failed to register the protocol handler for local forwarding channel",
						ex);
			}
			close();
		}
	}

	/**
	 * Either nothing was listening on the clients end of the tunnel, or the
	 * connection was rejected. Now we close the connection from the server to
	 * the start of the tunnel.
	 */
	@Override
	protected void onChannelOpenFailure() {
		try {
			socketChannel.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void initialize(ProtocolEngine engine, SshEngine daemon, SelectableChannel channel) {
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
	@Override
	protected byte[] openChannel(byte[] requestdata)
			throws WriteOperationRequest, ChannelOpenException {

		ByteArrayReader bar = new ByteArrayReader(requestdata);
		try {

			readForwarding(bar);

			boolean success = checkPermissions();

			if(Log.isDebugEnabled()) {
				Log.debug("Forwarding policy has "
						+ (success ? "authorized" : "denied") + " "
						+ connection.getUsername()
						+ (success ? " to open" : " from opening")
						+ " a " + getChannelType() + " forwarding channel to " + targetToConnect());
			}

			if (!success) {
				throw new ChannelOpenException("User does not have permission",
						ChannelOpenException.ADMINISTRATIVIVELY_PROHIBITED);
			}

			// Create and connect non-blocking socket channel
			socketChannel = createSocketChannel();
			if (socketChannel.connect(createSocketAddress())) {
				if(Log.isInfoEnabled()) {
					Log.info("Local forwarding socket to {} has connected channel={}", targetToConnect(), getLocalId());
				}
				hasConnected = true;
				return null;
			}

			if(Log.isTraceEnabled()) {
				Log.trace("Deferring socket connection on {} channel={}", targetToConnect(), getLocalId());
			}

			// Register the connector and we will confirm once weve connected
			connection.getContext().getEngine()
					.registerConnector(this, socketChannel);


		} catch (Throwable ex) {
			ex.printStackTrace();
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

	protected abstract void readForwarding(ByteArrayReader bar) throws IOException;

	protected abstract SocketAddress createSocketAddress();

	protected abstract SocketChannel createSocketChannel() throws IOException;

	protected abstract boolean checkPermissions();

	/**
	 * Called when the forwarded socket has been connected.
	 *
	 * @param key
	 *            SelectionKey
	 * @return boolean
	 */
	@Override
	public synchronized boolean finishConnect(SelectionKey key) {

		if (socketChannel == null) {
			return true;
		}

		if(hasConnected) {
			if(Log.isWarnEnabled()) {
				Log.warn("Duplicate finishConnect call to {} channel={}", targetToConnect(), getLocalId());
			}
			return true;
		}

		hasConnected = true;

		try {
			while (!socketChannel.finishConnect()) {
				// Wait for the connection to complete
			}
			if(Log.isInfoEnabled()) {
				Log.info("Local forwarding socket to {} has connected channel={}", targetToConnect(), getLocalId());
			}

			onConnectionComplete();

			connection.sendChannelOpenConfirmation(this, null);

		} catch (IOException ex) {
			if(Log.isInfoEnabled()) {
				Log.info(
						"Local forwarding socket to {} has failed: {} channel={}",
						targetToConnect(),
						ex.getMessage(),
						getLocalId());
			}

			onConnectionError(ex);

			connection.sendChannelOpenFailure(this,
					ChannelOpenException.CONNECT_FAILED, "Connection failed.");
		}

		return true;
	}

	protected abstract String targetToConnect();
}
