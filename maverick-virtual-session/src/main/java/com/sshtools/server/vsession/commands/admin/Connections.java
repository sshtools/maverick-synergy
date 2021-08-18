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

package com.sshtools.server.vsession.commands.admin;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Connections extends ShellCommand {

	public Connections() {
		super("con", ShellCommand.SUBSYSTEM_SYSTEM, "con", "Show active connections");
	}

	public void run(String[] args, VirtualConsole process)
			throws IOException, PermissionDeniedException {

		process.println(String.format("%s %16s %s", "UUID", "IP Address", "Username"));
		for(SshConnection c : process.getConnection().getConnectionManager().getAllConnections()) {
			process.println(String.format("%s %16s %s", c.getUUID(), c.getRemoteAddress().getAddress(), c.getUsername()));
		}
	}

}
