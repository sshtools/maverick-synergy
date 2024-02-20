package com.sshtools.server.vsession;

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
