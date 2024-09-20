package com.sshtools.server.callback;

/*-
 * #%L
 * Callback Server API
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
