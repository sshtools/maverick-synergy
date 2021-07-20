
package com.sshtools.common.ssh;

import java.util.Objects;

import com.sshtools.common.logger.Log;

public abstract class ConnectionAwareTask extends AbstractRequestFuture implements Runnable {

	protected final SshConnection con;
	private Throwable lastError;
	
	public ConnectionAwareTask(SshConnection con) {
		if(Objects.isNull(con)) {
			throw new IllegalArgumentException();
		}
		this.con = con;
	}
	
	protected abstract void doTask() throws Throwable;
	
	
	public final void run() {
		
		con.getConnectionManager().setupConnection(con);
		
		try {
			doTask();
			done(true);
		} catch(Throwable t) { 
			this.lastError = t;
			Log.error("Connection task failed with an error", t);
			done(false);
		} finally {
			con.getConnectionManager().clearConnection();
		}
	}

	public Throwable getLastError() {
		return lastError;
	}
}
