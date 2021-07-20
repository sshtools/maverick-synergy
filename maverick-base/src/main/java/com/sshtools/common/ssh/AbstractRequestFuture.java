/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

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
