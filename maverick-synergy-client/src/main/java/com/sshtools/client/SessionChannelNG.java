
package com.sshtools.client;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import com.sshtools.common.logger.Log;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.ssh.CachingDataWindow;

/**
 * Implements the client side of the SSH Connection protocol session channel
 */
public class SessionChannelNG extends AbstractSessionChannel implements SessionChannel {

	CachingDataWindow extendedData;
	ChannelInputStream stderrInputStream;
	
	public SessionChannelNG(SshConnection con) {
		this(con, false);
	}

	public SessionChannelNG(SshConnection con, boolean autoConsume) {
		this(con.getContext().getPolicy(ShellPolicy.class).getSessionMaxPacketSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMinWindowSize(),
				null, autoConsume);
	}
	
	public SessionChannelNG(int maximumPacketSize, int initialWindowSize, int maximumWindowSpace, int minimumWindowSpace,
			ChannelRequestFuture closeFuture, boolean autoConsume) {
		super(maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace, closeFuture, autoConsume);
		extendedData = new CachingDataWindow(maximumWindowSpace, true);
		stderrInputStream = new ChannelInputStream(extendedData);
	}

	public SessionChannelNG(int maximumPacketSize, int initialWindowSize, int maximumWindowSpace,
			int minimumWindowSpace, boolean autoConsume) {
		this(maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace, null, autoConsume);
	}

	public SessionChannelNG(int maximumPacketSize,
			int initialWindowSize, int maximumWindowSpace, int minimumWindowSpace) {
		this(maximumPacketSize, initialWindowSize,
				maximumWindowSpace, minimumWindowSpace, null, false);
	}

	@Override
	protected void onExtendedData(ByteBuffer data, int type) {
		if(type==SSH_EXTENDED_DATA_STDERR) {
			extendedData.put(data);
		}
	}
	
	public InputStream getStderrStream() {
		return stderrInputStream;
	}

	protected boolean checkWindowSpace() {
		if(Log.isTraceEnabled()) {
			Log.trace("Checking window space on channel=" + getLocalId() + " window=" + localWindow.getWindowSpace()
						+ (Objects.nonNull(cache) ? " cached=" + cache.remaining() : "")
						+ (Objects.nonNull(extendedData) ? " extended=" + extendedData.remaining() : ""));
		}
		return localWindow.getWindowSpace() 
				+ (Objects.nonNull(cache) ? cache.remaining() : 0) 
				+ (Objects.nonNull(extendedData) ? extendedData.remaining() : 0) 
				<= localWindow.getMinimumWindowSpace();
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
