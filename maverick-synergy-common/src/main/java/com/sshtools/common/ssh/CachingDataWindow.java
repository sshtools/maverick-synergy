package com.sshtools.common.ssh;

import java.nio.ByteBuffer;

import com.sshtools.common.logger.Log;

public class CachingDataWindow extends ChannelDataWindow {

	ByteBuffer cache;
	boolean blocking = false;
	boolean open = true;
	
	public CachingDataWindow(int initialWindowSpace, int maximumWindowSpace, int minimumWindowSpace, int maximumPacketSize) {
		super(initialWindowSpace, maximumWindowSpace, minimumWindowSpace, maximumPacketSize);
		cache = ByteBuffer.allocate(maximumWindowSpace);
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
		try {
				
			cache.compact();
			
			if(blocking) {
				while(cache.remaining() < data.remaining()) {
					cache.flip();
					wait(0);
					cache.compact();
				}
			}
			
			int remaining = data.remaining();
			
			cache.put(data);
			cache.flip();
			
			int count = remaining - data.remaining();
			if(Log.isDebugEnabled()) {
				Log.trace(String.format("Written %d bytes from cached data window position=%d remaining=%d limit=%d", 
						count, cache.position(), cache.remaining(), cache.limit()));
			}
			
			notifyAll();
		} catch (Exception e) {
			Log.error("Buffer overflow?", e);
		}
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
		if(Log.isDebugEnabled()) {
			Log.trace(String.format("Read %d bytes from cached data window position=%d remaining=%d limit=%d", 
					count, cache.position(), cache.remaining(), cache.limit()));
		}
		
		notifyAll();
		return count;
		
	}
	
	public synchronized int remaining() {
		return cache.remaining();
	}
	
	public synchronized boolean isAdjustRequired() {
		return windowSpace + remaining() < minimumWindowSpace;
	}
	
	public synchronized int getAdjustCount() {
		return maximumWindowSpace - windowSpace - remaining();
	}

	public synchronized boolean isOpen() {
		return open || cache.hasRemaining();
	}
}
