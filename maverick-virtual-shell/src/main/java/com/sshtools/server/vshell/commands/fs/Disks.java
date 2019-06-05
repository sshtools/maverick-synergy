package com.sshtools.server.vshell.commands.fs;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Disks extends ShellCommand {
	public Disks() {
		super("disks", SUBSYSTEM_FILESYSTEM, "[<path>]");
		setDescription("List local root file systems or displays information about local disk drives");
		getOptions().addOption("m", "mb", false, "Display size in megabytes");
		getOptions().addOption("k", "kb", false, "Display size in kilobytes");
		getOptions().addOption("g", "gb", false, "Display size in gigabytes");
		getOptions().addOption("b", "b", false, "Display size in bytes");
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException {
		String[] sArgs = args.getArgs();
		if (sArgs.length == 1) {
			for (File file : File.listRoots()) {
				process.getConsole().printStringNewline(file.getAbsolutePath());
			}
		} else {
			for (int i = 1; i < sArgs.length; i++) {
				File f = new File(sArgs[i]);
				process.getConsole().printStringNewline(f.getAbsolutePath());
				process.getConsole().printStringNewline("    Total: " + format(args, f.getTotalSpace()));
				process.getConsole().printStringNewline("    Usable: " + format(args, f.getUsableSpace()));
				process.getConsole().printStringNewline("    Free: " + format(args, f.getFreeSpace()));
			}
		}

	}

	static String format(CommandLine args, long value) {
		if (args.hasOption('m')) {
			return (value / 1024 / 1024) + " MB";
		} else if (args.hasOption('k')) {
			return (value / 1024) + " KB";
		} else if (args.hasOption('b')) {
			return value + " Bytes";
		} else {
			return (value / 1024 / 1024 / 1024) + " GB";
		}
	}
}
