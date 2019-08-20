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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.sshtools.common.ssh.CachingDataWindow;
import com.sshtools.common.ssh.ChannelDataWindow;
import com.sshtools.common.ssh.ChannelOutputStream;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SshConnection;

/**
 * Implements the client side of the SSH Connection protocol session channel
 */
public class SessionChannelNG extends AbstractSessionChannel implements SessionChannel {

	CachingDataWindow cached;
	ChannelInputStream channelInputStream;
	ChannelOutputStream channelOutputStream = new ChannelOutputStream(this);
	CachingDataWindow extendedData;
	ChannelInputStream stderrInputStream;

	public SessionChannelNG(SshConnection con, int maximumPacketSize, int initialWindowSize, int maximumWindowSpace, int minimumWindowSpace,
			ChannelRequestFuture closeFuture) {
		super(con, maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace, closeFuture);
	}

	public SessionChannelNG(SshConnection con, int maximumPacketSize, int initialWindowSize, int maximumWindowSpace,
			int minimumWindowSpace) {
		super(con, maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace);
	}

	@Override
	protected ChannelDataWindow createLocalWindow(int initialWindowSize, int maximumWindowSpace,
			int minimumWindowSpace, int maximumPacketSize) {
		extendedData = new CachingDataWindow(initialWindowSize, maximumWindowSpace, minimumWindowSpace, maximumPacketSize);
		stderrInputStream = new ChannelInputStream(extendedData);
		cached = new CachingDataWindow(initialWindowSize, maximumWindowSpace, minimumWindowSpace, maximumPacketSize);
		channelInputStream = new ChannelInputStream(cached);
		return cached;
	}
	
	@Override
	protected void onChannelData(ByteBuffer data) {
		synchronized (localWindow) {
			cached.put(data);
		}
	}

	@Override
	protected void onExtendedData(ByteBuffer data, int type) {
		if(type==SSH_EXTENDED_DATA_STDERR) {
			extendedData.put(data);
		}
	}
	
	public InputStream getInputStream() {
		return channelInputStream;
	}
	
	public InputStream getErrorStream() {
		return stderrInputStream;
	}
	
	public OutputStream getOutputStream() {
		return channelOutputStream;
	}
	
	protected boolean checkWindowSpace() {
		return cached.getWindowSpace() + cached.remaining() + extendedData.remaining() < cached.getMinimumWindowSpace();
	}

	@Override
	public int getMaximumWindowSpace() {
		return localWindow.getMaximumWindowSpace();
	}

	@Override
	public int getMinimumWindowSpace() {
		return localWindow.getMinimumWindowSpace();
	}

	@Override
	public void onSessionOpen() {
		
	}
}
