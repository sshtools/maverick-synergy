package com.sshtools.server.vsession.commands.sftp;

import java.util.Arrays;

public class SftpClientArguments {

	private int port = 22;
	private boolean compression;
	private String identityFile;
	private String[] ciphers;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public boolean isCompression() {
		return compression;
	}

	public void setCompression(boolean compression) {
		this.compression = compression;
	}

	public String getIdentityFile() {
		return identityFile;
	}

	public void setIdentityFile(String identityFile) {
		this.identityFile = identityFile;
	}
	
	public String[] getCiphers() {
		return ciphers;
	}

	public void setCiphers(String[] ciphers) {
		this.ciphers = ciphers;
	}

	@Override
	public String toString() {
		return "SftpClientArguments [port=" + port + ", compression=" + compression + ", identityFile=" + identityFile
				+ ", ciphers=" + Arrays.toString(ciphers) + "]";
	}

}
