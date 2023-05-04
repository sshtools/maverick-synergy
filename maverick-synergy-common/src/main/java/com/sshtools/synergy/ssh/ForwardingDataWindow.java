package com.sshtools.synergy.ssh;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ForwardingDataWindow extends CachingDataWindow {

	ForwardingDataWindow(int maximumWindowSpace) {
		super(maximumWindowSpace, true);
	}

	public synchronized int write(SocketChannel socketChannel) throws IOException {
		if(Boolean.getBoolean("maverick.disableMaximumWrite")) {
			return socketChannel.write(cache);
		} else {
			int c = 0;
			while(true) {
				int r = socketChannel.write(cache);
				if(r<=0) {
					break;
				}
				c+=r;
			}
			return c;
		}
	}
	
	public synchronized int read(SocketChannel socketChannel) throws IOException {
		
		cache.compact();
		
		try {
			return socketChannel.read(cache);
		} finally {
			cache.flip();
		}
	}
}
