
package com.sshtools.common.ssh;

public interface ExecutorOperationListener {

	void addedTask(Runnable r);
	
	void completedTask(Runnable r);

	void startedTask(Runnable r);
}
