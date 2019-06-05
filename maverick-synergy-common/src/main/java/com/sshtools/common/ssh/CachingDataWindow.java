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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
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
