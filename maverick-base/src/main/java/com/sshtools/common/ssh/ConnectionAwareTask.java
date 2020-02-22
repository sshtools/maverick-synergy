/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
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
