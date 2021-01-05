/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vsession;

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
