package com.sshtools.common.nio;

import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.common.ssh.Connection;

public class ConnectRequestFuture extends AbstractRequestFuture {

	ProtocolEngine transport;
	Connection<?> con;
	String host;
	int port;
	
	ConnectRequestFuture() {
	}
	
	ConnectRequestFuture(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public void connected(ProtocolEngine transport, Connection<?> con) {
		this.transport = transport;
		this.con = con;
		super.done(true);
	}
	
	void failed() {
		super.done(false);
	}
	
	public ProtocolEngine getTransport() {
		return transport;
	}
	
	public Connection<?> getConnection() {
		return con;
	}
	
	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
}
