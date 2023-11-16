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
		
		setEnv(console.getContext().getPolicy(VirtualSessionPolicy.class).getShellEnvironment());
		setDirectory(console.getContext().getPolicy(VirtualSessionPolicy.class).getShellDirectory());
		
		return args;
	}

}
