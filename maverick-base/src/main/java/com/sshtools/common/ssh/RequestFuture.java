package com.sshtools.common.ssh;

public interface RequestFuture {

	boolean isDone();
	
	boolean isSuccess();

	RequestFuture waitFor(long timeout);

	RequestFuture waitForever();
	
	void addFutureListener(RequestFutureListener listener);
}
