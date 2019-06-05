package com.sshtools.common.ssh;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRequestFuture implements RequestFuture {

	boolean done = false;
	boolean success = false;
	List<RequestFutureListener> listeners = new ArrayList<RequestFutureListener>();
	
	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public boolean isSuccess() {
		return success;
	}
	
	public synchronized void done(boolean success) {
		this.done = true;
		this.success = success;
		
		for(RequestFutureListener future : listeners) {
			future.complete(this);
		}
		
		notifyAll();
	}
	
	@Override
	public synchronized RequestFuture waitForever() {
		
		try {
			while(!done) {
				wait(100);
			}
		} catch (InterruptedException e) {
		}
		return this;
	}

	public synchronized RequestFuture waitFor(long timeout) {
		
		if(done) {
			return this;
		}
		try {
			wait(timeout);
		} catch (InterruptedException e) {
		}
		return this;
	}
	
	@Override
	public synchronized void addFutureListener(RequestFutureListener listener) {
		if(isDone()) {
			listener.complete(this);
		} else {
			listeners.add(listener);
		}
		
	}
}
