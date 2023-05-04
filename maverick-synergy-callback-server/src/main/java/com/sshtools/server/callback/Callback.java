package com.sshtools.server.callback;

import com.sshtools.client.tasks.Task;
import com.sshtools.common.ssh.SshConnection;

public interface Callback {

	String getUuid();

	String getUsername();

	SshConnection getConnection();

	String getRemoteAddress();

	String getMemo();

	Task addTask(Task task);

	void setMemo(String memo);

}
