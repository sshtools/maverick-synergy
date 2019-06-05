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
