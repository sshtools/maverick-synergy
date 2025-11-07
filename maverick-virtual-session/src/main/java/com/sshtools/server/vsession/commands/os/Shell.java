package com.sshtools.server.vsession.commands.os;

import static com.sshtools.server.vsession.commands.os.ShellUtils.findCommand;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/*-
 * #%L
 * Virtual Sessions
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.server.vsession.VirtualSessionPolicy;

public class Shell extends AbstractOSCommand {

	public static final String USER_HOME = "HOME";
	
	String commandProgram = "$SYSTEMROOT\\System32\\cmd.exe";
	String powershellProgram = "$SYSTEMROOT\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
	String powershellEncoding = "ISO-8859-1";
	String shellCommand = null;
	
	public Shell() {
		super("osshell", ShellCommand.SUBSYSTEM_SYSTEM, "osshell", "Run a native shell");
		setDescription("The current operating systems shell.");
		setBuiltIn(false);
	}
	
	public Shell(String name) {
		super(name, ShellCommand.SUBSYSTEM_SYSTEM, name, "Run a native shell");
		setDescription("The current operating systems shell.");
		setBuiltIn(false);
	}
	
	private String generateWindowsPath(String path) {
		return path.replace("$SYSTEMROOT", defaultIfBlank(System.getenv("SystemRoot"), "C:\\Windows"));
	}

	protected List<String> configureCommand(String cmd, List<String> cmdArgs, VirtualConsole console) throws IOException {
		
		
		
		List<String> args = new ArrayList<>();
		VirtualSessionPolicy policy = console.getContext().getPolicy(VirtualSessionPolicy.class);
		Optional<String> shellCommand = policy.shellCommand();
		if (SystemUtils.IS_OS_WINDOWS) {
			shellCommand.ifPresentOrElse(c -> {
				args.add(c);
				args.addAll(policy.getShellArguments());
			}, () -> {
				String path = generateWindowsPath(powershellProgram);
				if(Files.exists(Path.of(path))) {
					args.add(path);
				} else {
					args.add(generateWindowsPath(commandProgram));
				}
			});
		}
		else {
			
			if(SystemUtils.IS_OS_MAC_OSX) {
				if(shellCommand.isEmpty()) {
					shellCommand = ofNullable(findCommand("zsh", "/bin/zsh", "bash", "/usr/bin/bash", "/bin/bash", "sh", "/usr/bin/sh", "/bin/sh"));
				}
			} else {
				if(shellCommand.isEmpty()) {
					shellCommand = ofNullable(findCommand("bash", "/usr/bin/bash", "/bin/bash", "sh", "/usr/bin/sh", "/bin/sh"));
				}
			}
		
			args.add(shellCommand.orElseThrow(() -> new IOException("Cannot find shell.")));
			args.addAll(policy.getShellArguments());
		}
		
		setEnv(policy.getShellEnvironment());
		policy.shellDirectory().ifPresent(d -> setDirectory(d.toFile()));
		
		return args;
	}

	protected void beforeShellCommand(List<String> args, VirtualConsole console) throws IOException {
		
	}
	
	public String getShellCommand() {
		return shellCommand;
	}

}
