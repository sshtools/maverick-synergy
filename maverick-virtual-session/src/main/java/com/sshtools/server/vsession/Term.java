package com.sshtools.server.vsession;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.synergy.ssh.TerminalModes;
import com.sshtools.synergy.ssh.TerminalModes.Mode;

public class Term extends ShellCommand {

	public Term() {
		super("term",ShellCommand.SUBSYSTEM_SHELL, 
				UsageHelper.build("term"),
				"Output information about the pseudo terminal");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		
		TerminalModes modes = console.getPseudoTerminalModes();
		console.printfln("Type: %s", console.getTerminal().getType());
		console.println("Modes");
		console.println("----------------");
		for(Mode mode : modes.modes().keySet()) {
			console.printfln("%s: 0x%s", StringUtils.rightPad(mode.name(), 13), Integer.toHexString(modes.get(mode)));
		}
		console.println("----------------");
		
	}

}
