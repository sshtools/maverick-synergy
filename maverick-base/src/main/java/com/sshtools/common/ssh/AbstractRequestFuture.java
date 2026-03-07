package com.sshtools.common.ssh;

import java.time.Duration;

/*-
 * #%L
 * Base API
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

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRequestFuture implements RequestFuture {

	private volatile boolean done = false;
	private volatile boolean success = false;
	private List<RequestFutureListener> listeners = new ArrayList<RequestFutureListener>();
	
	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public boolean isSuccess() {
		return success;
	}
	
	public synchronized void done(boolean success) {

		this.done = true;
		this.success = success;
		
		for(RequestFutureListener future : listeners) {
			future.complete(this);
		}
		
		notifyAll();
	}
	
	@Override
	public synchronized RequestFuture waitIndefinitely() throws InterruptedException {
		while(!done) {
			wait(100);
		}
		return this;
	}

	@Override
	public synchronized RequestFuture waitFor(Duration timeout)  throws InterruptedException {
		
		if(done) {
			return this;
		}
		
		long timeoutMs = timeout.toMillis();
		long current = System.currentTimeMillis();
		long expected = current + timeoutMs - 10l;
		do {
			wait(timeoutMs <= 0l ? 10l : timeoutMs);
			long c = System.currentTimeMillis();
			timeoutMs -= (c - current);
			current = c;
		} while (!done && current < expected);
		return this;
	}
	
	@Override
	public synchronized void addFutureListener(RequestFutureListener listener) {
		if(isDone()) {
			listener.complete(this);
		} else {
			listeners.add(listener);
		}
		
	}

	@Override
	public void removeFutureListener(RequestFutureListener listener) {
		listeners.remove(listener);
	}
}
