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
