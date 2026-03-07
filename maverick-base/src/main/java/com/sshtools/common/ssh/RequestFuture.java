package com.sshtools.common.ssh;

import java.time.Duration;

import com.sshtools.common.logger.Log;

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

public interface RequestFuture {

	/**
	 * Get if the request has completed. 
	 * 
	 * @return done
	 */
	boolean isDone();
	
	/**
	 * Get if the request completed successfully. If the request is not yet done, this method may return false.
	 * 
	 * @return success success
	 */
	boolean isSuccess();
	
	/**
	 * Get if the request is done and successful. This is a convenience method that
	 * combines {@link #isDone()} and {@link #isSuccess()}.
	 * 
	 * @return true if the request is done and successful, false otherwise
	 */
	default boolean isDoneAndSuccess() {
		return isDone() && isSuccess();
	}

	/**
	 * Wait for the request to complete, up to the specified timeout. Any
	 * {@link InterruptedException} is swallowed and the method returns immediately
	 * if the thread is interrupted while waiting.
	 * 
	 * @param timeout
	 * @return this future
	 */
	default RequestFuture waitFor(long timeout) {
		try {
			return waitFor(Duration.ofMillis(timeout));
		} catch (InterruptedException e) {
			Log.debug("Interrupted while waiting for request to complete.");
			return this;
		}
	}

	/**
	 * Wait forever for the request to complete. Any {@link InterruptedException} is
	 * swallowed and the method returns immediately if the thread is interrupted
	 * while waiting.
	 * 
	 * @return this future
	 */
	default RequestFuture waitForever() {
		try {
			return waitIndefinitely();
		} catch (InterruptedException e) {
			Log.debug("Interrupted while waiting for request to complete.");
			return this;
		}
	}
	
	/**
	 * Wait for the request to complete, up to the specified timeout.
	 * 
	 * @param timeout
	 * @return this future
	 * @throws InterruptedException if the thread is interrupted while waiting
	 */
	RequestFuture waitFor(Duration timeout) throws InterruptedException;

	/**
	 * Wait forever for the request to complete. 
	 * 
	 * @return this future
	 * @throws InterruptedException if the thread is interrupted while waiting
	 */
	RequestFuture waitIndefinitely() throws InterruptedException;
	
	/**
	 * Add a listener to be notified when the request completes. The event is fired
	 * when {@link #isDone()} becomes true. If the request is already done when this
	 * method is called, the listener is notified immediately.
	 * 
	 * @param listener listener to add
	 */
	void addFutureListener(RequestFutureListener listener);
	
	/**
	 * Remove a listener from the list of listeners to be notified when the request
	 * completes. If the listener is not registered, this method has no effect.
	 * 
	 * @param listener listener to remove
	 */
	void removeFutureListener(RequestFutureListener listener);
}
