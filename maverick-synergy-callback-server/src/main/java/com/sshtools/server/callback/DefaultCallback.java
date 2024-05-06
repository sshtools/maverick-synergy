package com.sshtools.server.callback;
import com.sshtools.client.tasks.Task;
import com.sshtools.common.ssh.SshConnection;

public class DefaultCallback implements Callback {

	SshConnection con;
	String memo;
	
	DefaultCallback(SshConnection con, String memo) {
		this.con = con;
		this.memo = memo;
	}
	
	@Override
	public String getUuid() {
		return con.getUUID();
	}

	@Override
	public String getUsername() {
		return con.getUsername();
	}

	@Override
	public SshConnection getConnection() {
		return con;
	}

	@Override
	public String getRemoteAddress() {
		return con.getRemoteIPAddress();
	}

	@Override
	public String getMemo() {
		return memo;
	}

	@Override
	public Task addTask(Task task) {
		con.addTask(task);
		return task;
	}

	@Override
	public void setMemo(String memo) {
		this.memo = memo;
	}

}
