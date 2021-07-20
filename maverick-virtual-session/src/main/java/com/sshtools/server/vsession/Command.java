
package com.sshtools.server.vsession;

import java.io.IOException;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.sshtools.common.permissions.PermissionDeniedException;

public interface Command {

	public static final int STILL_ACTIVE = Integer.MIN_VALUE;
	
	public abstract void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException;

	public abstract String getDescription();

	public abstract String getSubsystem();

	public abstract String getCommandName();

	public abstract String getUsage();

	public abstract boolean isBuiltIn();

	public abstract int getExitCode();

	public abstract boolean isHidden();
	
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates);


}