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

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public abstract class AbstractCommand implements Command {
	private String name;
	private String subsystem;
	private String signature;
	private String description;
	
	private boolean builtIn = false;
	
	protected int exitCode = 0;
	
	
	public boolean isHidden() {
		return false;
	}

	public AbstractCommand(String name, String subsystem, String usage, String description) {
		this.name = name;
		this.subsystem = subsystem;
		this.signature = usage;
		this.description = description;
	}

	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}

	public String getSubsystem() {
		return subsystem;
	}

	public String getCommandName() {
		return name;
	}

	public String getUsage() {
		return signature;
	}

	public boolean isBuiltIn() {
		return builtIn;
	}

	protected void setBuiltIn(boolean builtIn) {
		this.builtIn = builtIn;
	}

	protected void setName(String name) {
		this.name = name;
	}

	protected void setSubsystem(String subsystem) {
		this.subsystem = subsystem;
	}

	protected void setSignature(String signature) {
		this.signature = signature;
	}

	protected void setDescription(String description) {
		this.description = description;
	}
	
	public int getExitCode() {
		return exitCode;
	}
	
	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		
	}
}
