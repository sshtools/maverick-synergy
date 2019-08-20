/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.nio;

import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Iterator;

import com.sshtools.common.logger.Log;

/**
 * Maintains a pool of {@link SelectoThread}s increasing the pool when required
 * and always maintaining the minimum number of permanent threads required.
 */
public class SelectorThreadPool {

	
	SelectorThreadImpl impl;
	ArrayList<SelectorThread> threads = new ArrayList<SelectorThread>();
	int permanentThreads;
	int maximumChannels;
	int nextAvailable;
	int idleServicePeriod;
	int inactivePeriodsPerIdleEvent;
	SelectorProvider selectorProvider;
	boolean isShuttingDown = false;
	boolean verbose = Boolean.getBoolean("maverick.verbose");
	
	/**
	 * Construct a thread pool. if(Log.isDebugEnabled()) Log.debug(
	 * 
	 * @param impl
	 *            SelectorThreadImpl
	 * @param permanentThreads
	 *            int
	 * @param maximumChannels
	 *            int
	 * @param idleServicePeriod
	 *            int
	 * @param inactivePeriodsPerIdleEvent
	 *            int
	 * @param selectorProvider
	 *            SelectorProvider
	 * @throws IOException
	 */
	public SelectorThreadPool(SelectorThreadImpl impl, int permanentThreads,
			int maximumChannels, int idleServicePeriod,
			int inactivePeriodsPerIdleEvent, SelectorProvider selectorProvider)
			throws IOException {
		this.impl = impl;
		this.permanentThreads = permanentThreads;
		this.maximumChannels = maximumChannels;
		this.idleServicePeriod = idleServicePeriod;
		this.inactivePeriodsPerIdleEvent = inactivePeriodsPerIdleEvent;
		this.selectorProvider = selectorProvider;

		if(verbose && Log.isDebugEnabled())
			Log.debug("Creating " + impl.getName() + " thread pool with "
					+ permanentThreads
					+ " permanent threads each with a maximum of "
					+ maximumChannels + " channels");

		for (int i = 0; i < permanentThreads; i++) {
			createThread();
		}

		nextAvailable = 0;
	}

	public void closeAllChannels() {
		// Stop any threads from being removed
		isShuttingDown = true;
		for (Iterator<SelectorThread> it = threads.iterator(); it.hasNext();) {
			SelectorThread t = it.next();
			t.closeAllChannels();
		}
	}
	/**
	 * Shutdown all threads in the pool.
	 */
	public synchronized void shutdown() {

		if(Log.isInfoEnabled()) {
			Log.info(String.format("Shutting down %s thread pool", impl.getName()));
		}
		
		isShuttingDown = true;
		for (Iterator<SelectorThread> it = threads.iterator(); it.hasNext();) {
			SelectorThread t = it.next();
			t.shutdown();
		}

		threads.clear();
	}

	void removeThread(SelectorThread thread) {
		if (!isShuttingDown) {
			threads.remove(thread);
			if(thread.isPermanent()) {
				try {
					createThread();
					if(Log.isWarnEnabled()) {
						Log.warn(String.format("A permanent thread was re-created because %s shutdown", thread.getName()));
					}
				} catch (IOException e) {
					Log.error("Failed to create replacement thread", e);
				}

			}
		}
	}

	private synchronized SelectorThread createThread() throws IOException {

		SelectorThread thread = new SelectorThread(this, impl,
				threads.size() < permanentThreads, maximumChannels,
				threads.size() + 1, idleServicePeriod,
				inactivePeriodsPerIdleEvent, selectorProvider);
		threads.add(thread);
		thread.start();

		return thread;
	}

	public synchronized int getCurrentLoad() {

		int count = 0;
		for (int i = 0; i < threads.size(); i++) {
			SelectorThread t = (SelectorThread) threads.get(i);
			count += t.getThreadLoad();
		}
		return count;
	}

	/**
	 * Select the next available thread with the minimum load.
	 * 
	 * @return SelectorThread
	 * @throws IOException
	 */
	public synchronized SelectorThread selectNextThread() throws IOException {

		int index = -1;
		int highestAvailableLoad = 0;
		SelectorThread t;
		int currentThreadsAvailableLoad;

		for (int i = 0; i < threads.size(); i++) {
			t = (SelectorThread) threads.get(i);
			currentThreadsAvailableLoad = t.getMaximumLoad()
					- t.getThreadLoad();
			if (currentThreadsAvailableLoad == t.getMaximumLoad()) {
				if(verbose && Log.isDebugEnabled())
					Log.debug("An idle thread has been selected id="
							+ t.getSelectorId());
				return t;
			}
			if(verbose && Log.isDebugEnabled())
				Log.debug("Thread id " + t.getSelectorId()
						+ " has a current load of " + t.getThreadLoad()
						+ " channels");

			if (currentThreadsAvailableLoad > 0
					&& currentThreadsAvailableLoad > highestAvailableLoad) {
				highestAvailableLoad = currentThreadsAvailableLoad;
				index = i;
			}
		}

		if (index > -1) {
			t = (SelectorThread) threads.get(index);
			if(verbose && Log.isDebugEnabled())
				Log.debug("Existing thread id " + t.getSelectorId()
						+ " selected with current load of " + t.getThreadLoad()
						+ " channels");
			return (SelectorThread) threads.get(index);
		}

		if(verbose && Log.isDebugEnabled())
			Log.debug("All threads are at maximum capacity");
		return createThread();

	}

}
