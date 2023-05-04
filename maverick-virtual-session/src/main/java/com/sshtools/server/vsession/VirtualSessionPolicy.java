package com.sshtools.server.vsession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VirtualSessionPolicy {

	String welcomeText = "Maverick Synergy\r\nVirtual Shell ${version}";
	String shellCommand = null;
	List<String> shellArguments = new ArrayList<>();
	
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
	
	
	
}
