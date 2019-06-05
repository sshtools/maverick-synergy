package com.sshtools.common.ssh;

import java.util.Objects;

public abstract class ConnectionAwareTask extends AbstractRequestFuture implements Runnable {

	SshConnection con;
	
	public ConnectionAwareTask(SshConnection con) {
		if(Objects.isNull(con)) {
			throw new IllegalArgumentException();
		}
		this.con = con;
	}
	
	protected abstract void doTask();
	
	
	public final void run() {
		
		con.getConnectionManager().setupConnection(con);
		
		try {
			doTask();
		} finally {
			con.getConnectionManager().clearConnection();
		}

	}

}
