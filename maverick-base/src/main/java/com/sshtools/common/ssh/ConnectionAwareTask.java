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
			done(getLastError()==null);
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
