
package com.sshtools.synergy.ssh;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

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
		return cache.hasRemaining();
	}

	public void close() {
		this.open = false;
	}
	
	public synchronized void put(ByteBuffer data) {
		
				
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

	public synchronized int get(byte[] tmp, int offset, int length) {
		
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
	
	public synchronized int get(ByteBuffer buffer) {
		
			
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
		return cache.remaining();
	}

	public synchronized boolean isOpen() {
		return open || cache.hasRemaining();
	}

	public synchronized void waitFor(long i) throws InterruptedException {
		wait(i);
	}
}
