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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class VirtualSessionPolicy {

	private String welcomeText = "Maverick Synergy\r\nVirtual Shell ${version}";
	private String shellCommand = null;
	private List<String> shellArguments = new ArrayList<>();
	private Map<String, String> shellEnvironment;
	private File shellDirectory;
	
	public VirtualSessionPolicy() {
		
	}
	
	public VirtualSessionPolicy(String welcomeText) {
		this.welcomeText = welcomeText;
	}

	public String getWelcomeText() {
		return welcomeText;
	}

	public void setWelcomeText(String welcomeText) {
		this.welcomeText = welcomeText;
	}

	public String getShellCommand() {
		return shellCommand;
	}
	
	public Collection<String> getShellArguments() {
		return shellArguments;
	}

	public void setShellCommand(String shellCommand) {
		this.shellCommand = shellCommand;
	}

	public Map<String, String> getShellEnvironment() {
		return shellEnvironment;
	}

	public File getShellDirectory() {
		return shellDirectory;
	}

	public void setShellDirectory(File shellDirectory) {
		this.shellDirectory = shellDirectory;
	}

	
}
