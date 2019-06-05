package com.sshtools.server.vshell;

import org.apache.commons.cli.Option;

public abstract class ShellCommand extends AbstractCommand {

	public ShellCommand(String name, String subsystem) {
		super(name, subsystem);
	}
	public ShellCommand(String name, String subsystem, String signature) {
		super(name, subsystem, signature, (Option[]) null);
	}

	public ShellCommand(String name, String subsystem, String signature, Option... options) {
		super(name, subsystem, signature, options);
	}
	
	public final static String SUBSYSTEM_SHELL = "Shell";
	public final static String SUBSYSTEM_SSHD = "Sshd";
	public final static String SUBSYSTEM_HELP = "Help";
	public static final String SUBSYSTEM_FILESYSTEM = "Filesystem";
	public static final String SUBSYSTEM_POLICY = "Policy";
	public static final String SUBSYSTEM_JVM = "JVM";
	public static final String SUBSYSTEM_SYSTEM = "System";
	public static final String SUBSYSTEM_TEXT_EDITING = "Text Editing";
	public static final String SUBSYSTEM_CALLBACK = "Callback";
	
	


}
