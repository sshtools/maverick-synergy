package com.sshtools.client;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ChannelDataWindow;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.SshConnection;

/**
 * Implements the client side of the SSH Connection protocol session channel
 */
public class AsyncSessionChannel extends AbstractSessionChannel {

	List<OutputStream> stderrListeners = new ArrayList<>();
	List<OutputStream> stdinListeners = new ArrayList<>();


	public AsyncSessionChannel(SshConnection con, int maximumPacketSize, int initialWindowSize, int maximumWindowSpace,
			int minimumWindowSpace, ChannelRequestFuture closeFuture) {
		super(con, maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace, closeFuture);
	}

	public AsyncSessionChannel(SshConnection con, int maximumPacketSize, int initialWindowSize, int maximumWindowSpace,
			int minimumWindowSpace) {
		super(con, maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace);
	}

	public void addStderrListener(OutputStream out) {
		synchronized(stderrListeners) {
			stderrListeners.add(out);
		}
	}
	
	public void addStdinListener(OutputStream out) {
		synchronized(stdinListeners) {
			stdinListeners.add(out);
		}
	}
	
	@Override
	protected ChannelDataWindow createLocalWindow(int initialWindowSize, int maximumWindowSpace,
			int minimumWindowSpace, int maximumPacketSize) {
		return new ChannelDataWindow(initialWindowSize,
				maximumWindowSpace, 
				minimumWindowSpace,
				maximumPacketSize);
	}

	@Override
	protected void onChannelClosed() {

		for(OutputStream out : stdinListeners) {
			try {
				out.close();
			} catch (IOException e) {
			}
		}
		
		for(OutputStream out : stderrListeners) {
			try {
				out.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	protected void onChannelData(ByteBuffer data) {
		
		byte[] tmp = new byte[data.remaining()];
		data.get(tmp);
		synchronized (stdinListeners) {
			for(OutputStream out : stdinListeners) {
				try {
					out.write(tmp);
				} catch (IOException e) {
					Log.warn("Caught exception whilst writing data to session outputstream", e);
				}
			}
		}
		
		localWindow.consume(tmp.length);
		evaluateWindowSpace();
	}

	@Override
	protected void onExtendedData(ByteBuffer data, int type) {
		
		byte[] tmp = new byte[data.remaining()];
		data.get(tmp);
		synchronized(stderrListeners) {
			for(OutputStream out : stderrListeners) {
				try {
					out.write(tmp);
				} catch (IOException e) {
					Log.warn("Caught exception whilst writing data to session error stream", e);
				}
			}
		}
		
		localWindow.consume(tmp.length);
		evaluateWindowSpace();
	}

	@Override
	protected boolean checkWindowSpace() {
		throw new UnsupportedOperationException();
	}

}
