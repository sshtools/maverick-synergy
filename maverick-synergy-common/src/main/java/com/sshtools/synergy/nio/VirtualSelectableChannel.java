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
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * An in-memory bidirectional channel that replaces a {@link java.nio.channels.SocketChannel}
 * for virtual (loopback) SSH connections.
 * <p>
 * Instances must be created in pairs via {@link #createPair(SocketAddress, SocketAddress)}.
 * Bytes written to one end appear in the read queue of the other end.  Registered
 * {@link VirtualSelector}s are notified via {@link VirtualSelector#notifyReady()} whenever
 * new data arrives.
 */
public class VirtualSelectableChannel extends AbstractSelectableChannel {

	private final LinkedBlockingDeque<ByteBuffer> inboundQueue = new LinkedBlockingDeque<>();
	private VirtualSelectableChannel peer;
	private final SocketAddress localAddress;
	private final SocketAddress remoteAddress;
	private volatile VirtualSelector registeredSelector;

	private VirtualSelectableChannel(VirtualSelectorProvider provider,
			SocketAddress localAddress, SocketAddress remoteAddress) {
		super(provider);
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
	}

	/**
	 * Create a connected pair of virtual channels.
	 *
	 * @param localAddr  the local address presented by channel[0]
	 * @param remoteAddr the remote address presented by channel[0] (and local address of channel[1])
	 * @return array of two connected channels: [0] = client side, [1] = server side
	 */
	public static VirtualSelectableChannel[] createPair(SocketAddress localAddr, SocketAddress remoteAddr) {
		VirtualSelectorProvider provider = VirtualSelectorProvider.getInstance();
		VirtualSelectableChannel a = new VirtualSelectableChannel(provider, localAddr, remoteAddr);
		VirtualSelectableChannel b = new VirtualSelectableChannel(provider, remoteAddr, localAddr);
		a.peer = b;
		b.peer = a;
		try {
			a.configureBlocking(false);
			b.configureBlocking(false);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to configure virtual channels as non-blocking", e);
		}
		return new VirtualSelectableChannel[] { a, b };
	}

	@Override
	public int validOps() {
		return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
	}

	@Override
	protected void implCloseSelectableChannel() throws IOException {
		// Wake up the peer's selector so it can detect the EOF
		VirtualSelectableChannel p = peer;
		if (p != null) {
			VirtualSelector ps = p.registeredSelector;
			if (ps != null) {
				ps.notifyReady();
			}
		}
	}

	@Override
	protected void implConfigureBlocking(boolean block) throws IOException {
		if (block) {
			throw new IOException("VirtualSelectableChannel does not support blocking mode");
		}
	}

	/**
	 * Read available data into {@code dst}.
	 *
	 * @return number of bytes read, 0 if no data is available, or -1 if the peer has closed
	 */
	public int read(ByteBuffer dst) throws IOException {
		if (!isOpen()) {
			return -1;
		}

		ByteBuffer head = inboundQueue.pollFirst();
		if (head == null) {
			// No data – return -1 if peer is gone, 0 otherwise
			VirtualSelectableChannel p = peer;
			if (p == null || !p.isOpen()) {
				return -1;
			}
			return 0;
		}

		int n = Math.min(head.remaining(), dst.remaining());
		int savedLimit = head.limit();
		head.limit(head.position() + n);
		dst.put(head);
		head.limit(savedLimit);

		if (head.hasRemaining()) {
			// Put the unconsumed remainder back at the front of the queue
			inboundQueue.addFirst(head);
		}
		return n;
	}

	/**
	 * Write {@code src} to the peer's inbound queue and notify its selector.
	 *
	 * @return number of bytes written, or -1 if the peer is closed
	 */
	public int write(ByteBuffer src) throws IOException {
		if (!isOpen()) {
			throw new IOException("Channel is closed");
		}
		VirtualSelectableChannel p = peer;
		if (p == null || !p.isOpen()) {
			return -1;
		}

		int n = src.remaining();
		// Copy the buffer so that the caller may reuse src immediately
		ByteBuffer copy = ByteBuffer.allocate(n);
		copy.put(src);
		copy.flip();
		p.enqueue(copy);
		p.notifySelector();
		return n;
	}

	/** @return {@code true} if there is data waiting to be read, or the peer has closed */
	public boolean isReadReady() {
		if (!inboundQueue.isEmpty()) {
			return true;
		}
		VirtualSelectableChannel p = peer;
		return p == null || !p.isOpen();
	}

	/** @return {@code true} if the peer is open and ready to accept writes */
	public boolean isWriteReady() {
		VirtualSelectableChannel p = peer;
		return p != null && p.isOpen();
	}

	/** Returns the local address for this end of the virtual connection. */
	public SocketAddress getLocalAddress() {
		return localAddress;
	}

	/** Returns the remote address (i.e. the peer's local address). */
	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	// -------------------------------------------------------------------------
	// Package-private helpers
	// -------------------------------------------------------------------------

	void enqueue(ByteBuffer buf) {
		inboundQueue.addLast(buf);
	}

	void setSelector(VirtualSelector selector) {
		this.registeredSelector = selector;
	}

	private void notifySelector() {
		VirtualSelector sel = registeredSelector;
		if (sel != null) {
			sel.notifyReady();
		}
	}
}
