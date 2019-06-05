package com.sshtools.common.ssh;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ForwardingDataWindow extends CachingDataWindow {

	ForwardingDataWindow(int initialWindowSpace, int maximumWindowSpace, int minimumWindowSpace, int maximumPacketSize) {
		super(initialWindowSpace, maximumWindowSpace, minimumWindowSpace, maximumPacketSize);
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
}
