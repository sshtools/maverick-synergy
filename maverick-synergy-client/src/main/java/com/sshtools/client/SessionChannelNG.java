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
package com.sshtools.client;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

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
