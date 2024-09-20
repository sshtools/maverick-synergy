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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.IdleStateManager;

/**
 * Provides management of a single selector.
 */
public class SelectorThread extends Thread {

	Selector selector;
	boolean running;
	LinkedList<Registration> pendingRegistrations;
	LinkedList<Runnable> pendingOperations;
	int maximumNumOfChannels;
	SelectorThreadImpl impl;
	SelectorThreadPool pool;
	boolean isPermanent;
	int id;
	static final int MAX_INACTIVITY = 1000;
	Object shutdownLock = new Object();
	SelectorProvider selectorProvider;
	IdleStateManager idleStates;
	boolean hasOperations = false;

	/**
	 * Construct a new selector thread.
	 * 
	 * @param pool
	 *            SelectorThreadPool
	 * @param impl
	 *            SelectorThreadImpl
	 * @param isPermanent
	 *            boolean
	 * @param maximumNumOfChannels
	 *            int
	 * @param id
	 *            int
	 * @param idleServicePeriod
	 *            int
	 * @param inactivePeriodsPerIdleEvent
	 *            int
	 * @param selectorProvider
	 *            SelectorProvider
	 * @throws IOException
	 */
	public SelectorThread(SelectorThreadPool pool, SelectorThreadImpl impl,
			boolean isPermanent, int maximumNumOfChannels, int id,
			int idleServicePeriod, int inactivePeriodsPerIdleEvent,
			SelectorProvider selectorProvider) throws IOException {
		this.pool = pool;
		this.impl = impl;
		this.isPermanent = isPermanent;
		this.id = id;
		this.maximumNumOfChannels = maximumNumOfChannels;
		this.selectorProvider = selectorProvider;
		this.idleStates = new IdleStateManager(idleServicePeriod,
				inactivePeriodsPerIdleEvent);

		pendingRegistrations = new LinkedList<Registration>();
		pendingOperations = new LinkedList<Runnable>();

		// Get a Selector object
		openSelector();

		setName(impl.getName() + "-" + id);
		setDaemon(true);

	}
	
	private void openSelector() throws IOException {
		
		if(selector!=null) {
			
			if(Log.isTraceEnabled()) {
				Log.trace("Opening new selector and transferring " + selector.keys().size() + " keys");
			}
			Selector newSelector = selectorProvider.openSelector();
			
			for(SelectionKey key : selector.keys()) {
				if(key.isValid()) {
					SelectionKey newKey = key.channel().register(newSelector, key.interestOps());
					newKey.attach(key.attachment());
					if(newKey.attachment() instanceof SelectionKeyAware) {
						((SelectionKeyAware)newKey.attachment()).setSelectionKey(newKey);
					}
				}
				key.cancel();
			}
			
			try {
				selector.select(50);
			} catch (Exception e1) {
			}
			try {
				selector.close();
			} catch (Exception e) {
			}
			
			this.selector = newSelector;
		} else {
			selector = selectorProvider.openSelector();
		}
	}

	IdleStateManager getIdleStates() {
		return idleStates;
	}

	/**
	 * Register a channel with the selector.
	 * 
	 * @param sc
	 *            SelectableChannel
	 * @param ops
	 *            int
	 * @param attachment
	 *            Object
	 * @param wakeUp
	 *            boolean
	 * @return boolean
	 * @throws ClosedChannelException
	 */
	public synchronized boolean register(SelectableChannel sc, int ops,
			Object attachment, boolean wakeUp) throws ClosedChannelException {

		if(Log.isTraceEnabled())
			Log.trace("Adding registration request to queue");

		synchronized (pendingRegistrations) {
			pendingRegistrations.addLast(new Registration(sc, ops, attachment));

		}

		if (wakeUp)
			selector.wakeup();

		return true;
	}

	private boolean performPendingRegistrations() {

		boolean hasRegistrations = !pendingRegistrations.isEmpty();

		if(!hasRegistrations) {
			return false;
		}
		
		synchronized (pendingRegistrations) {

			while (!pendingRegistrations.isEmpty()) {
				try {
					Registration reg = (Registration) pendingRegistrations
							.removeFirst();

					if(Log.isTraceEnabled())
						Log.trace("Registering channel with interested ops "
								+ reg.getInterestedOps());

					if (reg.getChannel().isOpen()) {

						if(Log.isTraceEnabled())
							Log.trace("Channel is open");

						SelectionKey key = reg.getChannel().register(selector,
								reg.getInterestedOps(), reg.getAttachment());

						if(Log.isTraceEnabled())
							Log.trace("Channel is registered");

						if (reg.getAttachment() instanceof SelectorRegistrationListener)
							((SocketHandler) reg.getAttachment())
									.registrationCompleted(reg.getChannel(),
											key, this);

						if(Log.isTraceEnabled())
							Log.trace("Registration complete");
					} else {
						if(Log.isTraceEnabled())
							Log.trace("Cannot register channel because it is closed!");
					}
				} catch (IOException ex) {
					if(Log.isTraceEnabled())
						Log.trace("Failed to register channel as it is closed");
				}
			}

		}

		return hasRegistrations;
	}

	public void closeAllChannels() {

		if(Log.isTraceEnabled()) {
			Log.trace(getName() + " closing all channels");
		}

		Iterator<SelectionKey> it = new ArrayList<SelectionKey>(selector.keys()).iterator();
		while (it.hasNext()) {
			SelectionKey key = it.next();
			Object obj = key.attachment();
			if (obj instanceof SocketConnection) {
				try {
					((SocketConnection) obj).socketChannel.close();
				} catch (IOException e) {
					// don't care
				}
				((SocketConnection) obj).protocolEngine.onSocketClose();
			}

			SelectableChannel channel = key.channel();

			try {
				synchronized (channel) {
					if (channel.isOpen()) {
						channel.close();
					}
				}
			} catch (IOException e) {
				// Ignored
			}

			// Ensure key is cancelled
			try {
				key.cancel();
			} catch (Throwable t) {
			}
		}
	}

	/**
	 * Add an operation to the selector.
	 * 
	 * @param r
	 *            Runnable
	 */
	public void addSelectorOperation(Runnable r) {

		synchronized (pendingOperations) {
			pendingOperations.addLast(r);
			hasOperations = true;
			wakeup();
		}

	}

	@SuppressWarnings("unchecked")
	private boolean performPendingOperations() {

		if(!hasOperations) {
			return false;
		}
		
		boolean hasRemaining = false;

		LinkedList<Runnable> ops = null;
		synchronized (pendingOperations) {
			if(!pendingOperations.isEmpty()) {
				hasRemaining = true;
				ops = (LinkedList<Runnable>)pendingOperations.clone();
				pendingOperations.clear();
				hasOperations = false;
			}
		}
		
		if(ops!=null) {
			while(!ops.isEmpty()) {
				Runnable r = (Runnable) ops.removeFirst();
				try {
					r.run();
				} catch(Throwable t) {
					if(Log.isErrorEnabled()) {
						Log.error("Consumed exception in pending operation", t);
					}
				}
			}
		}
		return hasRemaining;

	}

	/**
	 * Wakeup the selector.
	 */
	public void wakeup() {
		selector.wakeup();
	}

	/**
	 * Get the current thread load.
	 * 
	 * @return int
	 */
	public synchronized int getThreadLoad() {
		return selector.keys().size() + pendingRegistrations.size();
	}

	/**
	 * Is this a permanent thread?
	 * 
	 * @return boolean
	 */
	public boolean isPermanent() {
		return isPermanent;
	}

	/**
	 * Shutdown the thread.
	 */
	public void flagShutdown() {

		running = false;
		if (!Thread.currentThread().equals(this))
			selector.wakeup();
	}

	public void shutdown() {
		synchronized (shutdownLock) {

			if(Log.isTraceEnabled()) {
				Log.trace("Waiting for " + getName() + " to shutdown");
			}

			flagShutdown();

			try {
				shutdownLock.wait(30000L);
			} catch (InterruptedException e) {
			}

			if(Log.isTraceEnabled()) {
				Log.trace(getName() + " has shutdown");
			}
		}
	}

	/**
	 * Get the id of this selector thread.
	 * 
	 * @return int
	 */
	public int getSelectorId() {
		return id;
	}

	/**
	 * Get the maximum number of channels that this thread can service.
	 * 
	 * @return int
	 */
	public int getMaximumLoad() {
		return maximumNumOfChannels;

	}

	/**
	 * The threads main.
	 */
	public void run() {

		try {
			running = true;

			int n = 0;

			if(Log.isTraceEnabled())
				Log.trace("Starting "
						+ (isPermanent ? "permanent " : "temporary ")
						+ impl.getName() + " thread id=" + id);

			long lastSelectStarted;
			long tmpTime = System.currentTimeMillis();
			boolean simulateEpollBug = Boolean.getBoolean("maverick.simulateEpollBug");
			boolean workaroundEpollBug = Boolean.getBoolean("maverick.workaroundEpollBug");
			int numberOfZeroSelects = 0;
			while (running) {

				try {
					performPendingOperations();

					try {

						if(!workaroundEpollBug) {
							n = selector.select(MAX_INACTIVITY);
						} else {
							// Block until one of the registered sockets is ready to
							// be
							// operated
							// on in one of the registered modes without blocking
							lastSelectStarted = System.currentTimeMillis();
							n = selector.select(MAX_INACTIVITY);
							if(n==0 && (System.currentTimeMillis() - lastSelectStarted) < 100) {
								// Possible bug in select
								numberOfZeroSelects++;
								if(numberOfZeroSelects > 10) {
									// Create a new selector and transfer any keys over to it
									openSelector();
							        continue;
								}
							} else {
								numberOfZeroSelects = 0;
							}
							if(simulateEpollBug
									&& (System.currentTimeMillis() - tmpTime) > 60000) {
								openSelector();
								tmpTime = System.currentTimeMillis();
								continue;
							}
						}
					} catch (Exception csx) {
						// Defensive code, making sure that we have a valid
						// selector
						if (selector.isOpen()) {
							continue;
						}
						csx.printStackTrace();
						if(Log.isTraceEnabled())
							Log.trace("Failed to select", csx);
						break;
					}

					// Service the idle states if its ready
					//synchronized (idleStates) {
						if (idleStates.isReady()) {
							idleStates.service();
						}
					//}

					// Execute any runnables on this thread that may affect the
					// selector
					performPendingOperations();

					// Perform any pending registrations
					performPendingRegistrations();

					// Selector returned but nothing is ready. Could be from an
					// interrupt or timeout.
					if (n == 0) {
						if (selector.keys().size() == 0
								&& pendingRegistrations.size() == 0
								&& !isPermanent)
							flagShutdown();
						continue;
					}

					// We have some ready sockets, so iterate them
					Iterator<SelectionKey> it = selector.selectedKeys().iterator();
					while (it.hasNext()) {
						SelectionKey key = it.next();
						it.remove();
						
						// Make sure the key is still valid
						if (!key.isValid()) {
							if(Log.isTraceEnabled())
								Log.trace("Selector is not valid");
							continue;
						}

						if(Log.isTraceEnabled()) {
							Log.trace("Selected key");
						}
						
						impl.processSelectionKey(key, this);
						
					}

				} catch (Throwable ex) {
					if(Log.isErrorEnabled()) {
						Log.error("Selector thread encountered an error", ex);
					}
				}
			}

			if(Log.isTraceEnabled()) {
				Log.trace("Shutting down "
						+ (isPermanent ? "permanent " : "temporary ")
						+ impl.getName() + " thread id=" + id);
			}

			pool.removeThread(this);

			closeAllChannels();

			try {
				if(Log.isTraceEnabled())
					Log.trace(impl.getName()
							+ " performing final select to cancel all keys");
				selector.select(50);
				if(Log.isTraceEnabled())
					Log.trace(impl.getName() + " completed final select");
			} catch (Throwable t) {
				if(Log.isTraceEnabled())
					Log.trace(impl.getName()
							+ " exception occured in final select", t);
			}
		} finally {
			
			try {
				selector.close();
			} catch (IOException e) {
			}
			synchronized (shutdownLock) {
				shutdownLock.notifyAll();
			}
		}
	}

	public void cancelKey(SelectionKey key) {
		if(Log.isTraceEnabled())
			Log.trace("Selection key is being cancelled");
		key.cancel();
		if(Log.isTraceEnabled())
			Log.trace("Cancelled key");
	}
	
	class Registration {
		SelectableChannel channel;
		int interestedOps;
		Object attachment;

		Registration(SelectableChannel channel, int interestedOps,
				Object attachment) {
			this.channel = channel;
			this.interestedOps = interestedOps;
			this.attachment = attachment;
		}

		public SelectableChannel getChannel() {
			return channel;
		}

		public int getInterestedOps() {
			return interestedOps;
		}

		public Object getAttachment() {
			return attachment;
		}
	}

}
