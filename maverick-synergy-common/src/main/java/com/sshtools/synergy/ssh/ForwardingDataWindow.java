/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

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
