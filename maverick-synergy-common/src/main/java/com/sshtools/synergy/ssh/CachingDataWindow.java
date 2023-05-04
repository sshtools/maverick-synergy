/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.synergy.ssh;

import java.io.EOFException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

import com.sshtools.common.logger.Log;

public class CachingDataWindow {

	ByteBuffer cache;
	boolean blocking = false;
	boolean open = true;
	long timeout = 30000;
	public CachingDataWindow(int size, boolean blocking) {
		this.blocking = blocking;
		cache = ByteBuffer.allocate(size);
		cache.flip();
	}

	public synchronized void enableBlocking() {
		blocking = true;
	}
	
	public synchronized void disableBlocking() {
		blocking = false;
	}
	
	public synchronized boolean hasRemaining() {
		return Objects.nonNull(cache) && cache.hasRemaining();
	}

	public synchronized void close() {
		
		if(this.open) {
			this.open = false;
			if(!cache.hasRemaining()) {
				this.cache = null;
			}
		}
	}
	
	public synchronized void put(ByteBuffer data) throws EOFException {
		
		// Do not use isOpen as it checks for remaining too. If its closed, its closed
		// and should not accept any more data at all.
		if(!open) {
			throw new EOFException();
		}
		
		cache.compact();
		
		if(blocking) {
			long start = System.currentTimeMillis();
			while(cache.remaining() < data.remaining()) {
				cache.flip();
				try {
					wait(1000);
				} catch (InterruptedException e) {
					throw new IllegalStateException("Interrupted during cache put wait");
				}
				cache.compact();
				if(System.currentTimeMillis() - start > timeout) {
					throw new IllegalStateException(String.format("Timeout trying to put %d bytes into cache with %d remaining", 
							data.remaining(),
							cache.remaining()));
					
				}
			}
		}
		
		int remaining = data.remaining();
		
		if(remaining > cache.remaining()) {
			throw new BufferOverflowException();
		}
		
		cache.put(data);
		cache.flip();
		
		int count = remaining - data.remaining();
		if(Log.isTraceEnabled()) {
			Log.trace("Written {} bytes from cached data window position={} remaining={} limit={}", 
					count, cache.position(), cache.remaining(), cache.limit());
		}
		
		notifyAll();

	}

	public synchronized int get(byte[] tmp, int offset, int length) throws EOFException {
		
		verifyOpen();
		
		if(blocking) {
			while(!cache.hasRemaining() && open) {
				try {
					wait(1000);
				} catch (InterruptedException e) {
				}
			}
		}
		
		int count = Math.min(length, cache.remaining());
		int limit = cache.limit();
		cache.limit(cache.position() + count);
		cache.get(tmp, offset, count);
		cache.limit(limit);
		if(Log.isTraceEnabled()) {
			Log.trace("Read {} bytes from cached data window position={} remaining={} limit={}", 
					count, cache.position(), cache.remaining(), cache.limit());
		}
		
		notifyAll();
		return count;
		
	}
	
	private void verifyOpen() throws EOFException {
		if(!isOpen()) {
			this.cache = null;
			throw new EOFException();
		}
	}

	public synchronized int get(ByteBuffer buffer) throws EOFException {
		
		verifyOpen();
			
		if(blocking) {
			while(!cache.hasRemaining() && open) {
				try {
					wait(0);
				} catch (InterruptedException e) {
				}
			}
		}
		
		int count = Math.min(buffer.remaining(), cache.remaining());
		int limit = cache.limit();
		cache.limit(cache.position() + count);
		buffer.put(cache);
		cache.limit(limit);
		if(Log.isTraceEnabled()) {
			Log.trace("Read {} bytes from cached data window position={} remaining={} limit={}", 
					count, cache.position(), cache.remaining(), cache.limit());
		}
		
		notifyAll();
		return count;
		
	}
	
	public synchronized int remaining() {
		return Objects.nonNull(cache) ? cache.remaining() : 0;
	}

	public synchronized boolean isOpen() {
		return open || (Objects.nonNull(cache) && cache.hasRemaining());
	}

	public synchronized void waitFor(long i) throws InterruptedException {
		wait(i);
	}
}
