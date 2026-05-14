package com.sshtools.synergy.nio;

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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import com.sshtools.common.logger.Log;

/**
 * A {@link SocketConnection} that communicates over a {@link VirtualSelectableChannel}
 * rather than a real TCP socket.
 * <p>
 * Instances are created by {@link SshEngine#connectVirtual} and should never
 * be constructed directly.
 */
public class VirtualSocketConnection extends SocketConnection {

	private VirtualSelectableChannel virtualChannel;
	private SocketAddress virtualLocalAddress;
	private SocketAddress virtualRemoteAddress;

	/**
	 * Initialise this connection from a {@link VirtualSelectableChannel}.
	 * The parent {@link SocketConnection#initialize} is intentionally NOT called
	 * because it casts the channel to {@link java.nio.channels.SocketChannel}.
	 */
	@Override
	public void initialize(ProtocolEngine protocolEngine, SshEngine daemon,
			SelectableChannel channel) throws IOException {
		this.protocolEngine = protocolEngine;
		this.daemon = daemon;
		this.daemonContext = daemon.getContext();
		this.virtualChannel = (VirtualSelectableChannel) channel;
		this.virtualLocalAddress = virtualChannel.getLocalAddress();
		this.virtualRemoteAddress = virtualChannel.getRemoteAddress();
		// socketChannel intentionally left null – this is a virtual connection
	}

	// -------------------------------------------------------------------------
	// Address / port accessors – override to avoid touching the private fields
	// in SocketConnection that are set from the real SocketChannel.
	// -------------------------------------------------------------------------

	@Override
	public SocketAddress getLocalAddress() {
		return virtualLocalAddress;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return virtualRemoteAddress;
	}

	@Override
	public int getLocalPort() {
		if (virtualLocalAddress instanceof InetSocketAddress) {
			return ((InetSocketAddress) virtualLocalAddress).getPort();
		}
		return 0;
	}

	@Override
	public int getPort() {
		if (virtualRemoteAddress instanceof InetSocketAddress) {
			return ((InetSocketAddress) virtualRemoteAddress).getPort();
		}
		return 0;
	}

	// -------------------------------------------------------------------------
	// Lifecycle
	// -------------------------------------------------------------------------

	@Override
	protected boolean isConnected() {
		return virtualChannel != null && virtualChannel.isOpen()
				&& protocolEngine != null && protocolEngine.isConnected();
	}

	@Override
	public void closeConnection(boolean closeProtocol) {
		if (!closed) {
			if (virtualChannel != null && virtualChannel.isOpen()) {
				try {
					virtualChannel.close();
				} catch (IOException ex) {
					if (Log.isTraceEnabled()) {
						Log.trace("Exception closing virtual channel", ex);
					}
				}
			}
			if (closeProtocol) {
				if (Log.isTraceEnabled()) {
					Log.trace("Closing virtual protocol engine");
				}
				protocolEngine.onSocketClose();
			}
			closed = true;
		}
	}

	// -------------------------------------------------------------------------
	// I/O events
	// -------------------------------------------------------------------------

	@Override
	public synchronized boolean processReadEvent() {
		if (Log.isTraceEnabled()) {
			Log.trace("Processing virtual socket READ event");
		}

		try {
			if (!isConnected()) {
				return true;
			}

			if (socketDataIn == null) {
				socketDataIn = daemonContext.getBufferPool().get();
			}

			int numBytesRead = virtualChannel.read(socketDataIn);
			socketDataIn.flip();

			if (Log.isTraceEnabled()) {
				Log.trace("Read {} bytes from virtual channel", numBytesRead);
			}

			if (numBytesRead == -1) {
				if (Log.isTraceEnabled()) {
					Log.trace("Virtual channel peer closed – treating as EOF");
				}
				closeConnection();
				return true;
			}

			if (socketDataIn.hasRemaining()) {
				protocolEngine.onSocketRead(socketDataIn);
			}

			return !isConnected();

		} catch (Throwable ex) {
			if (Log.isDebugEnabled()) {
				Log.debug("Connection closed on virtual read: {}", ex.getMessage());
			}
			closeConnection();
			return true;
		} finally {
			if (socketDataIn != null) {
				if (!socketDataIn.hasRemaining()) {
					daemonContext.getBufferPool().add(socketDataIn);
					socketDataIn = null;
				} else {
					socketDataIn.compact();
				}
			}
		}
	}

	@Override
	public synchronized boolean processWriteEvent() {
		if (Log.isTraceEnabled()) {
			Log.trace("Processing virtual socket WRITE event");
		}

		if (virtualChannel == null || !virtualChannel.isOpen()) {
			return true;
		}

		if (socketDataOut == null) {
			socketDataOut = daemonContext.getBufferPool().get();
		}

		try {
			// Fill buffer from protocol engine if it wants to write and buffer is empty
			if (socketDataOut.remaining() == socketDataOut.capacity()
					&& protocolEngine.isConnected()) {
				SocketWriteCallback c = protocolEngine.onSocketWrite(socketDataOut);
				if (c != null) {
					socketWriteCallbacks.addLast(c);
				}
			}

			socketDataOut.flip();

			if (!virtualChannel.isOpen()) {
				return true;
			}

			if (socketDataOut.hasRemaining()) {
				int written = virtualChannel.write(socketDataOut);
				if (Log.isTraceEnabled()) {
					Log.trace("Written {} bytes to virtual channel", written);
				}
			}

			// Drain any read data that arrived while we were writing
			if (socketDataIn != null) {
				socketDataIn.flip();
				if (socketDataIn.hasRemaining()) {
					protocolEngine.onSocketRead(socketDataIn);
				}
			}

			return !isConnected();

		} catch (Throwable ex) {
			if (Log.isTraceEnabled()) {
				Log.trace("Connection closed on virtual write", ex);
			}
			closeConnection();
			return true;
		} finally {
			if (socketDataOut != null) {
				if (!socketDataOut.hasRemaining()) {
					daemonContext.getBufferPool().add(socketDataOut);
					socketDataOut = null;

					for (java.util.Iterator<SocketWriteCallback> it = socketWriteCallbacks.iterator();
							it.hasNext();) {
						it.next().completedWrite();
					}
					socketWriteCallbacks.clear();
				} else {
					socketDataOut.compact();
				}
			}

			if (socketDataIn != null) {
				if (!socketDataIn.hasRemaining()) {
					daemonContext.getBufferPool().add(socketDataIn);
					socketDataIn = null;
				} else {
					socketDataIn.compact();
				}
			}
		}
	}

	@Override
	public int getInitialOps() {
		return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
	}
}
