package com.sshtools.server.vshell.commands.fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.commands.AbstractFileCommand;

public class Cat extends AbstractFileCommand {
	public Cat() {
		super("cat", ShellCommand.SUBSYSTEM_SHELL, "cat <filename>...",
				new Option("E", false, "Display $ at end of each line"),
				new Option("n", "number", false, "number all output lines"),
				new Option("s", "squeeze-blank", false, "suppress repeated empty output lines"),
				new Option("T", "show-tabs", false, "displays TAB characters as ^I"),
				new Option("v", "show-nonprinting", false, "use ^ and M- notation, except for LFD and TAB"));
		setDescription("Lists the contents of a file");
		setBuiltIn(false);
	}

	public void run(CommandLine cli, VirtualProcess process) throws IOException, PermissionDeniedException {
		String[] args = cli.getArgs();
		if (args.length < 2)
			throw new IOException("At least one argument required");
		for (int i = 1; i < args.length; i++) {
			AbstractFile obj = process.getCurrentDirectory().resolveFile(args[i]);
			BufferedReader reader = new BufferedReader(new InputStreamReader(obj.getInputStream()));
			
			try {
				String line = null;
				int n = 1;
				boolean lastLineBlank = false;
				while ((line = reader.readLine()) != null) {
					if(lastLineBlank && line.equals("") && cli.hasOption('s')) {
						continue;
					}
					lastLineBlank = line.equals("");
					if(cli.hasOption('n')) {
						process.getConsole().printString("  " + n++ + " ");
					}
					if(cli.hasOption('v')) {
						for(int x=0;x<line.length();x++) {
							char c = line.charAt(x);
							if((c & 0x80) == 0x80) {
								c &= ~0x80;
							}
							if(c == 9 || c == 10) {
								process.getConsole().printCharacter(c);
							} else if(c < 32) {
								process.getConsole().printCharacter('^');
								process.getConsole().printCharacter(c+64);
							} else if(c < 127) {
								process.getConsole().printCharacter(c);
							} else {
								process.getConsole().printString("^?");
							}
						}
					} else {
						process.getConsole().printString(line);
					}
					if(cli.hasOption('E')) {
						process.getConsole().printStringNewline("$");
					} else {
						process.getConsole().printNewline();
					}
					process.getConsole().flushConsole();
				}
			} finally {
				reader.close();
			}
		}
	}
	

}
