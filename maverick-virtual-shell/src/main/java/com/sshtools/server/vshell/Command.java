package com.sshtools.server.vshell;

import java.io.IOException;

import jline.Completor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.sshtools.common.permissions.PermissionDeniedException;

public interface Command extends Completor {

	public static final int STILL_ACTIVE = Integer.MIN_VALUE;
	
	public abstract Options getOptions();

	public abstract void run(CommandLine args, VirtualProcess process)
			throws IOException, PermissionDeniedException;

	public abstract String getDescription();

	public abstract String getSubsystem();

	public abstract String getCommandName();

	public abstract String getSignature();

	public abstract boolean isBuiltIn();

	public abstract int getExitCode();

	public abstract void init(VirtualProcess process);

	public abstract boolean isHidden();


}