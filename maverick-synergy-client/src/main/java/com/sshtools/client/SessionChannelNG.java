/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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

import java.io.EOFException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import com.sshtools.common.logger.Log;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.UnsignedInteger32;
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
	
	public SessionChannelNG(int maximumPacketSize, UnsignedInteger32 initialWindowSize, UnsignedInteger32 maximumWindowSpace, UnsignedInteger32 minimumWindowSpace,
			ChannelRequestFuture closeFuture, boolean autoConsume) {
		super(maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace, closeFuture, autoConsume);
		extendedData = new CachingDataWindow(maximumWindowSpace.intValue(), true);
		stderrInputStream = new ChannelInputStream(extendedData);
	}

	public SessionChannelNG(int maximumPacketSize, UnsignedInteger32 initialWindowSize, UnsignedInteger32 maximumWindowSpace,
			UnsignedInteger32 minimumWindowSpace, boolean autoConsume) {
		this(maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace, null, autoConsume);
	}

	public SessionChannelNG(int maximumPacketSize,
			UnsignedInteger32 initialWindowSize, UnsignedInteger32 maximumWindowSpace, UnsignedInteger32 minimumWindowSpace) {
		this(maximumPacketSize, initialWindowSize,
				maximumWindowSpace, minimumWindowSpace, null, false);
	}

	@Override
	protected void onExtendedData(ByteBuffer data, int type) {
		
		super.onExtendedData(data, type);
		
		if(type==SSH_EXTENDED_DATA_STDERR) {
			try {
				extendedData.put(data);
			} catch (EOFException e) {
				Log.error("Attempt to write extended data to channel cache failed because the cache is closed");
				close();
			}
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
		return localWindow.getWindowSpace().longValue()
				+ (Objects.nonNull(cache) ? cache.remaining() : 0) 
				+ (Objects.nonNull(extendedData) ? extendedData.remaining() : 0) 
				<= localWindow.getMinimumWindowSpace().longValue();
	}
	
	@Override
	public UnsignedInteger32 getMaximumWindowSpace() {
		return localWindow.getMaximumWindowSpace();
	}

	@Override
	public UnsignedInteger32 getMinimumWindowSpace() {
		return localWindow.getMinimumWindowSpace();
	}

	@Override
	public void onSessionOpen() {
		
	}

}
