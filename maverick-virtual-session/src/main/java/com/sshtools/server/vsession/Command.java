package com.sshtools.server.vsession;

/*-
 * #%L
 * Virtual Sessions
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
