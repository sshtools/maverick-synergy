package com.sshtools.server.vsession.commands.sftp;

public class SftpClientArguments {

	private int port = 22;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "SftpClientArguments [port=" + port + "]";
	}
	
}
