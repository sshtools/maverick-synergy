package com.sshtools.server.vsession.commands;

import java.io.IOException;
import java.nio.file.Paths;

import org.jline.builtins.Options;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Nano extends ShellCommand {

	public Nano() {
		super("nano", ShellCommand.SUBSYSTEM_TEXT_EDITING, "Usage: nano", "");
	}

	@Override
	public void run(String[] args, VirtualConsole console) throws IOException, PermissionDeniedException {
		
		Options opt = Options.compile(args);
		org.jline.builtins.Nano edit = new org.jline.builtins.Nano(console.getTerminal(), Paths.get(""));
        edit.open(opt.args());
        edit.run();
	}

}
