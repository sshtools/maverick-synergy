package com.sshtools.server.vshell.commands.fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Follow extends ShellCommand {
	public Follow() {
		super("follow", ShellCommand.SUBSYSTEM_FILESYSTEM, "<filename>");
		setDescription("Monitor a file");
		setBuiltIn(false);
	}

	public void run(CommandLine cli, VirtualProcess process)
			throws IOException, PermissionDeniedException {
		String[] args = cli.getArgs();
		if (args.length != 2)
			throw new IOException("A single argument is required");
		try {
			AbstractFile obj = process.getCurrentDirectory().resolveFile(
					args[1]);
			long _filePointer = 0;
			while (true) {
				Thread.sleep(2000);
				long len = obj.getAttributes().getSize().longValue();
				if (len < _filePointer) {
					process.getConsole()
							.printStringNewline(
									"--- TRUNCATED ---   File was reset. Restarting following from start of file.");
					_filePointer = len;
				} else if (len > _filePointer) {
					InputStream in = obj.getInputStream();
					try {
						in.skip(_filePointer);
						BufferedReader r = new BufferedReader(
								new InputStreamReader(in));
						String line = null;
						while ((line = r.readLine()) != null) {
							process.getConsole().printStringNewline(line);
						}
						_filePointer = len;
					} finally {
						in.close();
					}
				}
			}
		} catch (Exception e) {
			process.getConsole()
					.printStringNewline(
							"--- ERROR ---   Fatal error following file, following stopped.");
		}
	}
}