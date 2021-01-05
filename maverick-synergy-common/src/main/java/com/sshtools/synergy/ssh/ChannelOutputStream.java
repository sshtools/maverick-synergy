/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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

import java.io.IOException;
import java.io.OutputStream;

public class ChannelOutputStream extends OutputStream {

		int type = -1;
		
		ChannelNG<?> channel;
		boolean sentEOF;
		
		public ChannelOutputStream(ChannelNG<?> channel) {
			this.channel = channel;
		}
		
		public ChannelOutputStream(ChannelNG<?> channel, int type) {
			this.type = type;
			this.channel = channel;
		}
		
		@Override
		public void write(int b) throws IOException {
			if(type > -1) {
				channel.sendExtendedData(new byte[] { (byte) b}, type);
			} else {
				channel.sendChannelDataAndBlock(new byte[] { (byte) b});
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if(type > -1) {
				channel.sendExtendedData(b, off, len, type);
			} else {
				channel.sendData(b, off, len);
			}
		}
		
		public void close() {
			synchronized(channel) {
				if(!sentEOF && !channel.isClosed() && !channel.isClosing()) {
					channel.sendEOF();
					sentEOF = true;
				}
			}
		}
	}