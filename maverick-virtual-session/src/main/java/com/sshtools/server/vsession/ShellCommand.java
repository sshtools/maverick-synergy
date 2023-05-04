package com.sshtools.server.vsession;

public abstract class ShellCommand extends AbstractCommand {

	public ShellCommand(String name, String subsystem, String signature, String description) {
		super(name, subsystem, signature, description);
	}

	public final static String SUBSYSTEM_SHELL = "Shell";
	public final static String SUBSYSTEM_SSHD = "Sshd";
	public final static String SUBSYSTEM_HELP = "Help";
	public static final String SUBSYSTEM_FILESYSTEM = "File System";
	public static final String SUBSYSTEM_POLICY = "Policy";
	public static final String SUBSYSTEM_JVM = "JVM";
	public static final String SUBSYSTEM_SYSTEM = "System";
	public static final String SUBSYSTEM_TEXT_EDITING = "Text Editing";
	public static final String SUBSYSTEM_CALLBACK = "Callback";
	
	


}
