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
