package com.sshtools.synergy.ssh;

public class RemoteForward {

	String hostToConnect;
	int portToConnect;
	
	public RemoteForward(String hostToConnect, int portToConnect) {
		this.hostToConnect = hostToConnect;
		this.portToConnect = portToConnect;
	}
	
	public String getHostToConnect() {
		return hostToConnect;
	}
	public int getPortToConnect() {
		return portToConnect;
	}
	
	
}
