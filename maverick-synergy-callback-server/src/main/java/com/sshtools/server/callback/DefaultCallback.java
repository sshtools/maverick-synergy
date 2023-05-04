/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.server.callback;
import com.sshtools.client.tasks.Task;
import com.sshtools.common.ssh.SshConnection;

public class DefaultCallback implements Callback {

	SshConnection con;
	String memo = "Undefined";
	
	DefaultCallback(SshConnection con) {
		this.con = con;
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
