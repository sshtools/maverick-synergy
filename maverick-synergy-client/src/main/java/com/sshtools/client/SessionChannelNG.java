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
