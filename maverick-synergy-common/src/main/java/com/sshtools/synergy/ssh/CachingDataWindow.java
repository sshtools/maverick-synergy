package com.sshtools.synergy.ssh;

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

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

import com.sshtools.common.logger.Log;

public class CachingDataWindow {

	ByteBuffer cache;
	boolean blocking = false;
	boolean open = true;
	long timeout = 30000;
	ChannelNG<?> channel;
	public CachingDataWindow(long size, boolean blocking, ChannelNG<?> channel) {
		this.blocking = blocking;
		this.channel = channel;
		cache = ByteBuffer.allocate((int)size);
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
	
	public void put(ByteBuffer data) throws IOException {
		
		while(data.remaining() > 0) {				
			synchronized(this) {
				if(!open) {
					throw new EOFException();
				}
				cache.compact();
//				if(cache.remaining() > data.remaining()) {
//					cache.put(data);
//					cache.flip();
//					notifyAll();
//					break;
//				} else {
					int max = Math.min(cache.remaining(), data.remaining());
					if(max > 0) {
						int tmp = data.limit();
						cache.put((ByteBuffer) data.limit(data.position() + max));
						data.limit(tmp);
	
						cache.flip();
						
						if(Log.isTraceEnabled()) {
							Log.trace("Written {} bytes from cached data window position={} remaining={} limit={}", 
									max, cache.position(), cache.remaining(), cache.limit());
						}
						
						notifyAll();
						
						
					}
					
					if(!data.hasRemaining()) {
						break;
					}
//				}
			
				if(blocking && data.hasRemaining()) {
					long start = System.currentTimeMillis();
					try {
						wait(1000);
					} catch (InterruptedException e) {
						throw new IllegalStateException("Interrupted during cache put wait");
					}
					
					if(System.currentTimeMillis() - start > timeout) {
						throw new IllegalStateException(String.format("Timeout trying to put %d bytes into cache with %d remaining", 
								data.remaining(),
								cache.remaining()));
						
					}
				} else if(!blocking && data.hasRemaining()) {
					throw new BufferOverflowException();
				}
			}
		}
	}

	public synchronized int get(byte[] tmp, int offset, int length) throws IOException {
		
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
		
		if(Objects.nonNull(channel)) {
			channel.consumeWindowSpace(count);
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

	public synchronized int get(ByteBuffer buffer) throws IOException {
		
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
		if(Objects.nonNull(channel)) {
			channel.consumeWindowSpace(count);
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
