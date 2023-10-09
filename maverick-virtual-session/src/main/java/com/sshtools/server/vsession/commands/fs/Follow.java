package com.sshtools.server.vsession.commands.fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Follow extends ShellCommand {
	public Follow() {
		super("follow", ShellCommand.SUBSYSTEM_FILESYSTEM, "follow <filename>", "Monitor a file");
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole process)
			throws IOException, PermissionDeniedException {

		if (args.length != 2)
			throw new IOException("A single argument is required");
		try {
			AbstractFile obj = process.getCurrentDirectory().resolveFile(
					args[1]);
			long _filePointer = 0;
			while (true) {
				Thread.sleep(2000);
				long len = obj.getAttributes().size().longValue();
				if (len < _filePointer) {
					process.println(
									"--- TRUNCATED ---   File was reset. Restarting following from start of file.");
					_filePointer = len;
				} else if (len > _filePointer) {
					InputStream in = obj.getInputStream();
					in.skip(_filePointer);
					try(BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
						String line = null;
						while ((line = r.readLine()) != null) {
							process.println(line);
						}
						_filePointer = len;
					} finally {
						in.close();
					}
				}
			}
		} catch (Exception e) {
			process.println("--- ERROR ---   Fatal error following file, following stopped.");
		}
	}
}
