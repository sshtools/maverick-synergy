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
