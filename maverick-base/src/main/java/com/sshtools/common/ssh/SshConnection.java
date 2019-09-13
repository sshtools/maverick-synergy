/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.ssh;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Locale;

import com.sshtools.common.events.EventListener;
import com.sshtools.common.logger.Log.Level;

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

	Context getContext();

	void executeTask(Runnable r);

	SshConnectionManager getConnectionManager();

	boolean isConnected();

	void addTask(Integer queue, ConnectionAwareTask connectionAwareTask);

	String getServerVersion();

	void openChannel(Channel channel);

	String getUUID();

	String getRemoteIdentification();

	void startLogging() throws IOException;
	
	void startLogging(Level trace) throws IOException;

	void disconnect();

	void addEventListener(EventListener listener);

	void removeEventListener(EventListener listener);

	void addTask(ConnectionAwareTask r);

}
