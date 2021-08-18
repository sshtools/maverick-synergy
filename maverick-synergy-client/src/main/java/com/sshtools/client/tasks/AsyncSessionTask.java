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

package com.sshtools.client.tasks;

import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.synergy.ssh.Connection;

/**
 * An  task for using the SSH session asynchronously
 */
public abstract class AsyncSessionTask implements Runnable {

	Connection<SshClientContext> con;
	long timeout = 10000;
	SessionChannelNG session;
	ChannelRequestFuture future;
	
	public AsyncSessionTask(Connection<SshClientContext> con, ChannelRequestFuture future) {
		this.con = con;
		this.future = future;
	}
	
	public AsyncSessionTask(Connection<SshClientContext> con) {
		this(con, new ChannelRequestFuture());
	}
	
	protected boolean isAllocatePseudoTerminal() {
		return true;
	}

	public void disconnect() {
		con.disconnect();
	}
	
	public ChannelRequestFuture getChannelFuture() {
		return future;
	}
	
	@Override
	public void run() {

		session = new SessionChannelNG(
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxPacketSize(), 
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMinWindowSize(),
				future,
				false);
		session.addEventListener(new ChannelEventListener() {

			@Override
			public void onChannelClose(Channel channel) {
				onCloseSession((SessionChannel)channel);
			}
		});
		
		con.getConnectionProtocol().openChannel(session);
		if(!session.getOpenFuture().waitFor(timeout).isSuccess()) {
			throw new IllegalStateException("Could not open session channel");
		}
		
		setupSession(session);

		onOpenSession(session);
	
	}
	
	protected int getBufferSize() {
		return 65535;
	}
	
	public OutputStream getOutputStream() {
		return session.getOutputStream();
	}
	
	public InputStream getInputStream() {
		return session.getInputStream();
	}
	
	protected abstract void setupSession(SessionChannel session);
	
	protected abstract void onOpenSession(SessionChannel session);
	
	protected abstract void onCloseSession(SessionChannel session);

	public void close() {
		session.close();
	}
	
}
