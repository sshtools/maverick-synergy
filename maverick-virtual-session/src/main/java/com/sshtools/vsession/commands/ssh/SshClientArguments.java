package com.sshtools.vsession.commands.ssh;

import java.util.Arrays;

public class SshClientArguments {

	private int port = 22;
	private String destination;
	private String identityFile;
	private String loginName;
	private String command;
	private String[] ciphers;
	private String[] hmacs;
	private String securityLevel;
	

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getIdentityFile() {
		return identityFile;
	}

	public void setIdentityFile(String identityFile) {
		this.identityFile = identityFile;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String[] getCiphers() {
		return ciphers;
	}

	public void setCiphers(String[] ciphers) {
		this.ciphers = ciphers;
	}

	public String[] getHmacs() {
		return hmacs;
	}

	public void setHmacs(String[] hmacs) {
		this.hmacs = hmacs;
	}

	public String getSecurityLevel() {
		return securityLevel;
	}

	public void setSecurityLevel(String securityLevel) {
		this.securityLevel = securityLevel;
	}

	@Override
	public String toString() {
		return "SshClientArguments [port=" + port + ", destination=" + destination + ", identityFile=" + identityFile
				+ ", loginName=" + loginName + ", command=" + command + ", ciphers=" + Arrays.toString(ciphers)
				+ ", hmacs=" + Arrays.toString(hmacs) + ", securityLevel=" + securityLevel + "]";
	}

}
