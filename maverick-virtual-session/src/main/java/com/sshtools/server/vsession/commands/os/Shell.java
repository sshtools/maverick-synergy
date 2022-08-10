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
package com.sshtools.server.vsession.commands.os;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.server.vsession.VirtualSessionPolicy;

public class Shell extends AbstractOSCommand {

	public Shell() {
		super("osshell", ShellCommand.SUBSYSTEM_SYSTEM, "osshell", "Run a native shell");
		setDescription("The current operating systems shell");
		setBuiltIn(false);
	}

	protected List<String> configureCommand(String cmd, List<String> cmdArgs, VirtualConsole console) throws IOException {
		
		List<String> args = new ArrayList<>();
		String shellCommand = console.getContext().getPolicy(VirtualSessionPolicy.class).getShellCommand();
		if (SystemUtils.IS_OS_WINDOWS) {
			if(StringUtils.isBlank(shellCommand)) {
				args.add("C:\\Windows\\System32\\cmd.exe");
			} else {
				args.add(shellCommand);
				args.addAll(console.getContext().getPolicy(VirtualSessionPolicy.class).getShellArguments());
			}
		}
		else {
			
			if(SystemUtils.IS_OS_MAC_OSX) {
				if(StringUtils.isBlank(shellCommand)) {
					shellCommand = findCommand("zsh", "/bin/zsh", "bash", "/usr/bin/bash", "/bin/bash", "sh", "/usr/bin/sh", "/bin/sh");
					if(shellCommand == null)
						throw new IOException("Cannot find OSX shell.");
				}
			} else {
				if(StringUtils.isBlank(shellCommand)) {
					shellCommand = findCommand("bash", "/usr/bin/bash", "/bin/bash", "sh", "/usr/bin/sh", "/bin/sh");
					if(shellCommand == null)
						throw new IOException("Cannot find shell.");
				}
			}
		
			args.add(shellCommand);
			args.addAll(console.getContext().getPolicy(VirtualSessionPolicy.class).getShellArguments());
		}
		
		return args;
	}

}