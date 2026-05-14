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

import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;

/**
 * A {@link SelectionKey} implementation for use with {@link VirtualSelector}
 * and {@link VirtualSelectableChannel}.
 */
class VirtualSelectionKey extends AbstractSelectionKey {

	private final AbstractSelectableChannel channel;
	private final VirtualSelector selector;
	private volatile int interestOps;
	private volatile int readyOps;

	VirtualSelectionKey(AbstractSelectableChannel channel, VirtualSelector selector,
			int interestOps, Object attachment) {
		this.channel = channel;
		this.selector = selector;
		this.interestOps = interestOps;
		this.readyOps = 0;
		attach(attachment);
	}

	@Override
	public java.nio.channels.SelectableChannel channel() {
		return channel;
	}

	@Override
	public java.nio.channels.Selector selector() {
		return selector;
	}

	@Override
	public int interestOps() {
		return interestOps;
	}

	@Override
	public SelectionKey interestOps(int ops) {
		if ((ops & ~channel.validOps()) != 0)
			throw new IllegalArgumentException("Invalid ops: " + ops);
		interestOps = ops;
		return this;
	}

	@Override
	public int readyOps() {
		return readyOps;
	}

	void setReadyOps(int ops) {
		this.readyOps = ops;
	}
}
