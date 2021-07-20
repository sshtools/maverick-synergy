
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