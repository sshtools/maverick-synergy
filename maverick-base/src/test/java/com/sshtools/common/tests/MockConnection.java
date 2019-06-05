package com.sshtools.common.tests;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshConnectionManager;

public class MockConnection implements SshConnection {

	boolean connected = true;
	String username;
	String sessionId;
	InetSocketAddress localAddress;
	InetSocketAddress remoteAddress;
	Map<String,Object> properties = new HashMap<>();
	
	public MockConnection(String username, String sessionId, InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
		this.username = username;
		this.sessionId = sessionId;
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
	}
	
	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public Object getProperty(String key) {
		return properties.get(key);
	}

	@Override
	public void setProperty(String key, Object value) {
		properties.put(key, value);
	}

	@Override
	public boolean containsProperty(String key) {
		return properties.containsKey(key);
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	@Override
	public Locale getLocale() {
		return Locale.getDefault();
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public void disconnect(String message) {
		connected = false;
	}
	
	@Override
	public void disconnect(int reason, String message) {
		disconnect(message);
	}
 
	@Override
	public InetAddress getRemoteAddress() {
		return remoteAddress.getAddress();
	}

	@Override
	public InetAddress getLocalAddress() {
		return localAddress.getAddress();
	}

	@Override
	public int getLocalPort() {
		return localAddress.getPort();
	}
	
	@Override
	public int getRemotePort() {
		return remoteAddress.getPort();
	}

	@Override
	public boolean isAuthenticated() {
		return !Objects.isNull(username);
	}

	@Override
	public AbstractFileFactory<?> getFileFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Context getContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void executeTask(Runnable r) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SshConnectionManager getConnectionManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public void addTask(Integer queue, ConnectionAwareTask connectionAwareTask) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getServerVersion() {
		return "1.2.3";
	}

	@Override
	public void openChannel(Channel channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getUUID() {
		return sessionId;
	}
	

}
