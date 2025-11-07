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

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.nio.ClientConnector;
import com.sshtools.synergy.nio.ProtocolEngine;
import com.sshtools.synergy.nio.SocketHandler;
import com.sshtools.synergy.nio.SshEngine;

public abstract class AbstractRemoteSocketForwardingChannel<T extends SshContext> extends SocketForwardingChannel<T>
		implements SocketHandler, ClientConnector{
	protected boolean hasConnected = false;

	public AbstractRemoteSocketForwardingChannel(String channeltype, SshConnection con) {
		super(channeltype, con);
	}

	@Override
	public void initialize(ProtocolEngine engine, SshEngine daemon, SelectableChannel channel) {
	}

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
				Log.warn("Duplicate finishConnect call to {} channel={}", targetToConnect(),
					getLocalId());
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
					Log.info("Remote forwarding socket to {} has connected [asynchronously] channel={} remote={}",
							targetToConnect(),
							getLocalId(),
							getRemoteId());
				}
			}

			connection.sendChannelOpenConfirmation(this, null);

		} catch (IOException ex) {
			if(Log.isInfoEnabled()) {
				Log.info("Remote forwarding socket to {} has failed \"{}\" channel={} remote={}",
							targetToConnect(),
								ex.getMessage(),
								getLocalId(),
								getRemoteId());
			}
			connection.sendChannelOpenFailure(this,
					ChannelOpenException.CONNECT_FAILED, "Connection failed.");
		}

		return true;
	}

	@Override
	protected void onRegistrationComplete() {
		if(Log.isTraceEnabled()) {
			Log.trace("Registration Complete channel={}", getLocalId());
		}
	}

	@Override
	protected void onChannelOpenConfirmation() {
		// Register the handler
		try {
			getContext().getEngine().registerHandler(this, socketChannel);
		} catch (IOException ex) {
			if(Log.isTraceEnabled()) {
				Log.trace("Failed to register channel with a selector", ex);
			}
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

	protected abstract SocketAddress createSocketAddress();

	protected abstract void createSocketChannel() throws IOException;

	protected abstract String targetToConnect();
}
