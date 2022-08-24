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
import java.io.OutputStream;

import com.sshtools.common.logger.Log;

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
					if(Log.isDebugEnabled()) {
						channel.log("The channel's OutputStream has been closed");
					}
					channel.sendEOF();
					sentEOF = true;
				}
			}
		}
	}