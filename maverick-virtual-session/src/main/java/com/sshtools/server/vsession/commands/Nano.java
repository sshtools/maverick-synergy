package com.sshtools.server.vsession.commands;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sshtools.common.files.nio.AbstractFileURI;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Nano extends ShellCommand {

	public Nano() {
		super("nano", "File System", "Usage: nano <file>", "File editor");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		
		Map<String,Object> env = new HashMap<>();
		env.put("connection", console.getConnection());
		FileSystem fs = FileSystems.newFileSystem(AbstractFileURI.create(console.getConnection(), ""), env);

		org.jline.builtins.Nano n = new org.jline.builtins.Nano(console.getTerminal(), fs.getPath(""));
		
		List<String> arglist = new ArrayList<>();
		if(args.length > 1) {
			for(int i=1;i<args.length;i++) {
				arglist.add(args[i]);
			}
		}


		n.open(arglist);
		n.run();
		
	}

}
