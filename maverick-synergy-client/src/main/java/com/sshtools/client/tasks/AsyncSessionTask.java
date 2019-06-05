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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client.tasks;

import java.io.OutputStream;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.ChannelEventAdapter;
import com.sshtools.common.ssh.ChannelOutputStream;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.SessionChannel;

/**
 * An  task for using the SSH session asynchronously
 */
public abstract class AsyncSessionTask implements Runnable {

	Connection<SshClientContext> con;
	long timeout = 10000;
	SessionChannelNG session;
	ChannelRequestFuture future;
	
	ChannelOutputStream sessionOut;
	
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
				con,
				con.getContext().getSessionMaxPacketSize(), 
				con.getContext().getSessionMaxWindowSize(),
				con.getContext().getSessionMaxWindowSize(),
				con.getContext().getSessionMinWindowSize(),
				future);
		session.addEventListener(new ChannelEventAdapter() {

			@Override
			public void onChannelClose(Channel channel) {
				onCloseSession((SessionChannel)channel);
			}
		});
		
		con.getConnectionProtocol().openChannel(session);
		if(!session.getOpenFuture().waitFor(timeout).isSuccess()) {
			throw new IllegalStateException("Could not open session channel");
		}
		
		this.sessionOut = new ChannelOutputStream(session);
		
		setupSession(session);

		onOpenSession(session);
	
	}
	
	protected int getBufferSize() {
		return 65535;
	}
	
	public OutputStream getOutputStream() {
		return sessionOut;
	}
	
	protected abstract void setupSession(SessionChannel session);
	
	protected abstract void onOpenSession(SessionChannel session);
	
	protected abstract void onCloseSession(SessionChannel session);

	public void close() {
		session.close();
	}
	
}
