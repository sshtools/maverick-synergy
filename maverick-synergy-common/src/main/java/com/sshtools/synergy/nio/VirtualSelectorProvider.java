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
import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

/**
 * A {@link SelectorProvider} that creates {@link VirtualSelector} instances.
 * <p>
 * Used to drive virtual (in-memory) SSH connections through the existing
 * Maverick Synergy NIO selector thread infrastructure without any OS sockets.
 * <p>
 * Only {@link #openSelector()} is supported; all other factory methods throw
 * {@link UnsupportedOperationException}.
 */
public class VirtualSelectorProvider extends SelectorProvider {

	private static final VirtualSelectorProvider INSTANCE = new VirtualSelectorProvider();

	/** Returns the singleton instance. */
	public static VirtualSelectorProvider getInstance() {
		return INSTANCE;
	}

	private VirtualSelectorProvider() {
	}

	@Override
	public AbstractSelector openSelector() throws IOException {
		return new VirtualSelector(this);
	}

	@Override
	public DatagramChannel openDatagramChannel() {
		throw new UnsupportedOperationException("VirtualSelectorProvider does not support DatagramChannel");
	}

	@Override
	public DatagramChannel openDatagramChannel(ProtocolFamily family) {
		throw new UnsupportedOperationException("VirtualSelectorProvider does not support DatagramChannel");
	}

	@Override
	public Pipe openPipe() {
		throw new UnsupportedOperationException("VirtualSelectorProvider does not support Pipe");
	}

	@Override
	public ServerSocketChannel openServerSocketChannel() {
		throw new UnsupportedOperationException("VirtualSelectorProvider does not support ServerSocketChannel");
	}

	@Override
	public SocketChannel openSocketChannel() {
		throw new UnsupportedOperationException("VirtualSelectorProvider does not support SocketChannel");
	}
}
