package com.sshtools.common.ssh;

import java.net.InetAddress;
import java.util.Locale;

import com.sshtools.common.files.AbstractFileFactory;

public interface SshConnection {

	String getUsername();

	Object getProperty(String key);

	void setProperty(String key, Object value);

	boolean containsProperty(String key);

	String getSessionId();

	Locale getLocale();

	void setUsername(String username);

	void disconnect(String message);
	void disconnect(int reason, String message);
	
	InetAddress getRemoteAddress();

	InetAddress getLocalAddress();

	int getLocalPort();
	
	int getRemotePort();

	boolean isAuthenticated();

	AbstractFileFactory<?> getFileFactory();

	Context getContext();

	void executeTask(Runnable r);

	SshConnectionManager getConnectionManager();

	boolean isConnected();

	void addTask(Integer queue, ConnectionAwareTask connectionAwareTask);

	String getServerVersion();

	void openChannel(Channel channel);

	String getUUID();

}
