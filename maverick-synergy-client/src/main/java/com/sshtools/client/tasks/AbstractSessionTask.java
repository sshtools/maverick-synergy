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

import java.io.IOException;
import java.util.Objects;

import com.sshtools.client.AbstractSessionChannel;
import com.sshtools.client.SshClient;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;

/**
 * An abstract task for using the SSH session
 */
public abstract class AbstractSessionTask<T extends AbstractSessionChannel> extends Task {

	long timeout = 10000;
	T session;
	ChannelRequestFuture future;
	Throwable lastError;
	
	public AbstractSessionTask(SshClient ssh, ChannelRequestFuture future) {
		super(ssh);
		this.future = future;
	}
	
	public AbstractSessionTask(SshConnection con, ChannelRequestFuture future) {
		super(con);
		this.future = future;
	}
	
	public AbstractSessionTask(SshConnection con) {
		this(con, new ChannelRequestFuture());
	}

	public T getSession() {
		return session;
	}
	
	public void disconnect() {
		con.disconnect();
	}
	
	public final Throwable getLastError() {
		return lastError;
	}
	
	public ChannelRequestFuture getChannelFuture() {
		return future;
	}
	
	@Override
	public void doTask() {

		session = createSession(con);
		
		con.openChannel(session);
		if(!session.getOpenFuture().waitFor(timeout).isSuccess()) {
			throw new IllegalStateException("Could not open session channel");
		}
		
		setupSession(session);
	

		try {
			if(Log.isDebugEnabled()) {
				Log.debug("Starting session task");
			}
			onOpenSession(session);
		} catch(Throwable ex) {
			this.lastError = ex;
		}
	
		if(Log.isDebugEnabled()) {
			Log.debug("Ending session task");
		}
		
		session.close();
		onCloseSession(session);
		
		done(Objects.isNull(lastError));
		
		if(Log.isDebugEnabled()) {
			Log.debug("Session task is done success={}", String.valueOf(Objects.isNull(lastError)));
		}
	}
	
	protected abstract T createSession(SshConnection con);
	
	protected abstract void setupSession(T session);
	
	protected abstract void onOpenSession(T session) throws IOException, SshException, ShellTimeoutException;
	
	protected abstract void onCloseSession(T session);

	public void close() {
		session.close();
	}

	public boolean isClosed() {
		return session.isClosed();
	}

	public void changeTerminalDimensions(int cols, int rows, int width, int height) {
		session.changeTerminalDimensions(cols, rows, width, height);
	}
	
}
