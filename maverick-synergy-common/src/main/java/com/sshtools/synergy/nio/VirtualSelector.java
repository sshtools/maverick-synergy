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
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link java.nio.channels.Selector} implementation that operates entirely
 * in memory, driving {@link VirtualSelectableChannel} instances without any
 * OS-level I/O.
 * <p>
 * Readiness is determined by querying each channel's
 * {@link VirtualSelectableChannel#isReadReady()} /
 * {@link VirtualSelectableChannel#isWriteReady()} methods.  Channels notify
 * this selector via {@link #notifyReady()} when new data is enqueued, causing
 * a blocked {@link #select(long)} to return early.
 */
public class VirtualSelector extends AbstractSelector {

	/** Default wait time for an unbounded {@code select()} call (ms). */
	private static final long UNBOUNDED_WAIT_MS = 1000L;

	private final Set<SelectionKey> keys = new HashSet<>();
	private final Object keysLock = new Object();

	private final Set<SelectionKey> selectedKeys = new HashSet<>();

	/** Lock used for wait/notify between writer threads and the selector loop. */
	private final Object notifyLock = new Object();

	/**
	 * Set to {@code true} by {@link #wakeup()} so that the next
	 * {@link #doSelect(long)} returns immediately even if called before
	 * {@code notifyLock.wait()} is entered.
	 */
	private final AtomicBoolean wokenUp = new AtomicBoolean(false);

	VirtualSelector(VirtualSelectorProvider provider) {
		super(provider);
	}

	// -------------------------------------------------------------------------
	// AbstractSelector abstract methods
	// -------------------------------------------------------------------------

	@Override
	protected void implCloseSelector() throws IOException {
		wakeup();
	}

	@Override
	protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
		VirtualSelectionKey key = new VirtualSelectionKey(ch, this, ops, att);
		((VirtualSelectableChannel) ch).setSelector(this);
		synchronized (keysLock) {
			keys.add(key);
		}
		return key;
	}

	// -------------------------------------------------------------------------
	// Selector abstract methods
	// -------------------------------------------------------------------------

	@Override
	public Set<SelectionKey> keys() {
		synchronized (keysLock) {
			// Remove any cancelled keys before returning the snapshot
			keys.removeIf(k -> !k.isValid());
			return Collections.unmodifiableSet(new HashSet<>(keys));
		}
	}

	@Override
	public Set<SelectionKey> selectedKeys() {
		return selectedKeys;
	}

	@Override
	public int selectNow() throws IOException {
		return doSelect(0);
	}

	@Override
	public int select(long timeout) throws IOException {
		return doSelect(timeout > 0 ? timeout : UNBOUNDED_WAIT_MS);
	}

	@Override
	public int select() throws IOException {
		return doSelect(UNBOUNDED_WAIT_MS);
	}

	@Override
	public java.nio.channels.Selector wakeup() {
		wokenUp.set(true);
		synchronized (notifyLock) {
			notifyLock.notifyAll();
		}
		return this;
	}

	// -------------------------------------------------------------------------
	// Package-private helpers
	// -------------------------------------------------------------------------

	/**
	 * Called by {@link VirtualSelectableChannel} when new data is placed into
	 * a channel's inbound queue.  Unblocks any thread waiting inside
	 * {@link #doSelect(long)}.
	 */
	void notifyReady() {
		synchronized (notifyLock) {
			notifyLock.notifyAll();
		}
	}

	// -------------------------------------------------------------------------
	// Internal
	// -------------------------------------------------------------------------

	private int doSelect(long timeout) {
		synchronized (notifyLock) {
			processCancelledKeys();

			// Check for immediately ready channels
			if (populateSelected() > 0) {
				return selectedKeys.size();
			}

			// selectNow() – return immediately even if nothing is ready
			if (timeout == 0) {
				return 0;
			}

			// If wakeup() was called before we entered wait, honour it now
			if (wokenUp.compareAndSet(true, false)) {
				return 0;
			}

			try {
				notifyLock.wait(timeout);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			processCancelledKeys();
			populateSelected();
			wokenUp.set(false);
			return selectedKeys.size();
		}
	}

	/**
	 * Inspects every registered key and adds those whose channel reports
	 * readiness (given current interest ops) to {@link #selectedKeys}.
	 *
	 * @return the number of newly-added ready keys
	 */
	private int populateSelected() {
		int added = 0;
		synchronized (keysLock) {
			for (SelectionKey key : keys) {
				if (!key.isValid()) continue;

				VirtualSelectableChannel vch = (VirtualSelectableChannel) key.channel();
				int readyOps = 0;

				if ((key.interestOps() & SelectionKey.OP_READ) != 0 && vch.isReadReady()) {
					readyOps |= SelectionKey.OP_READ;
				}
				if ((key.interestOps() & SelectionKey.OP_WRITE) != 0 && vch.isWriteReady()) {
					readyOps |= SelectionKey.OP_WRITE;
				}

				if (readyOps != 0) {
					((VirtualSelectionKey) key).setReadyOps(readyOps);
					if (selectedKeys.add(key)) {
						added++;
					}
				}
			}
		}
		return added;
	}

	/** Drains the cancelled-key set maintained by {@link AbstractSelector}. */
	@SuppressWarnings("unchecked")
	private void processCancelledKeys() {
		Set<AbstractSelectionKey> cancelled = (Set<AbstractSelectionKey>) (Set<?>) cancelledKeys();
		synchronized (cancelled) {
			if (!cancelled.isEmpty()) {
				synchronized (keysLock) {
					keys.removeAll(cancelled);
				}
				selectedKeys.removeAll(cancelled);
				cancelled.clear();
			}
		}
	}
}
