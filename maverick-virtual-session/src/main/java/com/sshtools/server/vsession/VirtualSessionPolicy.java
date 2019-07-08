package com.sshtools.server.vsession;

public class VirtualSessionPolicy {

	String welcomeText = "Maverick Synergy\r\nVirtual Shell ${version}";

	public String getWelcomeText() {
		return welcomeText;
	}

	public void setWelcomeText(String welcomeText) {
		this.welcomeText = welcomeText;
	}
	
	
	
	
}
