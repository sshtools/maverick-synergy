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

package com.sshtools.server.vsession.commands;

import java.io.IOException;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.AbstractUUIDCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Kill extends AbstractUUIDCommand {
	public Kill() {
		super("kill", SUBSYSTEM_SHELL, "kill [<loginId or processId>]", "Kill a process or login");
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {

		if (args.length < 2) {
			throw new IOException("Not enough arguments.");
		} else {
			for (int i = 1; i < args.length; i++) {
				try {
					long pid = Long.parseLong(args[i]);
					console.getShell().killProcess(pid);
				}
				catch(NumberFormatException nfe) {
					SshConnection connection = console.getConnection().getConnectionManager().getConnectionById(args[i]);
					if(connection!=null)
						connection.disconnect("Killed by " + console.getConnection().getUsername());
				}
			}				
		}
	}
}
