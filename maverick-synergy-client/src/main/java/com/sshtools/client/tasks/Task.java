package com.sshtools.client.tasks;

import com.sshtools.client.SshClient;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshConnection;

public abstract class Task extends ConnectionAwareTask implements Runnable {
	@FunctionalInterface
	public interface TaskRunnable<T> {
		void run(T self) throws Exception;
	}

	public static <T> Task ofRunnable(SshConnection con, TaskRunnable<T> runnable) {
		return new Task(con) {
			
			@SuppressWarnings("unchecked")
			@Override
			protected void doTask() throws Throwable {
				runnable.run((T)this);
			}
		};
	}
	
	public Task(SshConnection con) {
		super(con);
	}
	
	public Task(SshClient ssh) {
		super(ssh.getConnection());
	}

}
