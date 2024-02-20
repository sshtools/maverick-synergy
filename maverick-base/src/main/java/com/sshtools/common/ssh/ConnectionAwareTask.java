package com.sshtools.common.ssh;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.Objects;

import com.sshtools.common.logger.Log;

public abstract class ConnectionAwareTask extends AbstractRequestFuture implements Runnable {

	protected final SshConnection con;
	protected Throwable lastError;
	
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
