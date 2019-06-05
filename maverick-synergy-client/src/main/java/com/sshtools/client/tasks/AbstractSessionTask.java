package com.sshtools.client.tasks;

import com.sshtools.client.AbstractSessionChannel;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventAdapter;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.Connection;

/**
 * An abstract task for using the SSH session
 */
public abstract class AbstractSessionTask<T extends AbstractSessionChannel> extends Task {

	Connection<SshClientContext> con;
	long timeout = 10000;
	T session;
	ChannelRequestFuture future;
	
	public AbstractSessionTask(Connection<SshClientContext> con, ChannelRequestFuture future) {
		super(con);
		this.con = con;
		this.future = future;
	}
	
	public AbstractSessionTask(Connection<SshClientContext> con) {
		this(con, new ChannelRequestFuture());
	}

	public T getSession() {
		return session;
	}
	
	public void disconnect() {
		con.disconnect();
	}
	
	public ChannelRequestFuture getChannelFuture() {
		return future;
	}
	
	@Override
	public void doTask() {

		session = createSession(con);
		
		session.addEventListener(new ChannelEventAdapter() {

			@Override
			public void onChannelClose(Channel channel) {
				onCloseSession(session);
			}
			
		});
		
		con.getConnectionProtocol().openChannel(session);
		if(!session.getOpenFuture().waitFor(timeout).isSuccess()) {
			throw new IllegalStateException("Could not open session channel");
		}
		
		setupSession(session);
		onOpenSession(session);
	
	}
	
	protected abstract T createSession(Connection<SshClientContext> con);
	
	protected abstract void setupSession(T session);
	
	protected abstract void onOpenSession(T session);
	
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
