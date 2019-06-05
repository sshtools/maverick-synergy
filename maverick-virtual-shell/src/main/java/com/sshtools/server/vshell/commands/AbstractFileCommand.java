package com.sshtools.server.vshell.commands;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.Option;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;

public abstract class AbstractFileCommand extends ShellCommand {

	public AbstractFileCommand(String name, String subsystem) {
		super(name, subsystem);
	}

	public AbstractFileCommand(String name, String subsystem, String signature,
			Option... options) {
		super(name, subsystem, signature, options);
	}

	public AbstractFileCommand(String name, String subsystem, String signature) {
		super(name, subsystem, signature);
	}

	public int complete(String buffer, int cursor, List<String> candidates) {
		
		try {
			AbstractFile pwd = getProcess().getCurrentDirectory();
			for(AbstractFile f : pwd.getChildren()) {
				if(f.getName().startsWith(buffer)) {
					candidates.add(f.getName());
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PermissionDeniedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

}
