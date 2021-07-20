
package com.sshtools.server.vsession.commands.fs;

import java.io.File;
import java.io.IOException;

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Disks extends ShellCommand {
	public Disks() {
		super("disks", SUBSYSTEM_FILESYSTEM, UsageHelper.build("disks [options] <path>", 
				"-m               Display size in megabytes",
				"-k               Display size in kilobytes",
				"-g               Display size in gigabytes",
				"-b               Display size in bytes"),
				"List local root file systems or displays information about local disk drives");

	}

	public void run(String[] args, VirtualConsole process) throws IOException {

		if (args.length == 1) {
			for (File file : File.listRoots()) {
				process.println(file.getAbsolutePath());
			}
		} else {
			for (int i = 1; i < args.length; i++) {
				File f = new File(args[i]);
				process.println(f.getAbsolutePath());
				process.println("    Total: " + format(args, f.getTotalSpace()));
				process.println("    Usable: " + format(args, f.getUsableSpace()));
				process.println("    Free: " + format(args, f.getFreeSpace()));
			}
		}

	}

	static String format(String[] args, long value) {
		if (CliHelper.hasShortOption(args, 'm')) {
			return (value / 1024 / 1024) + " MB";
		} else if (CliHelper.hasShortOption(args, 'k')) {
			return (value / 1024) + " KB";
		} else if (CliHelper.hasShortOption(args, 'b')) {
			return value + " Bytes";
		} else {
			return (value / 1024 / 1024 / 1024) + " GB";
		}
	}
}
