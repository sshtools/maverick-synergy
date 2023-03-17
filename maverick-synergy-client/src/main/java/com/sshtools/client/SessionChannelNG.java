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
