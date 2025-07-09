package com.sshtools.common.ssh;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.net.InetAddress;
import java.util.Locale;

import com.sshtools.common.events.EventListener;
import com.sshtools.common.events.EventTrigger;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.ssh.components.SshPublicKey;


public interface SshConnection extends EventTrigger {

	String getUsername();

	Object getProperty(String key);

	void setProperty(String key, Object value);

	boolean containsProperty(String key);

	String getSessionId();

	Locale getLocale();

	void setUsername(String username);

	void disconnect(String message);
	
	void disconnect(int reason, String message);

	InetAddress getLocalAddress();

	int getLocalPort();
	
	int getRemotePort();

	boolean isAuthenticated();

	Context getContext();

	void executeTask(Runnable r);

	SshConnectionManager getConnectionManager();

	boolean isConnected();

	boolean isDisconnecting();

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

	ConnectionAwareTask addTask(ConnectionAwareTask r);

	String[] getRemotePublicKeys();

	String[] getRemoteKeyExchanges();

	String[] getRemoteCompressionsCS();

	String[] getRemoteCompressionsSC();

	String[] getRemoteCiphersCS();

	String[] getRemoteCiphersSC();

	String[] getRemoteMacsCS();

	String[] getRemoteMacsSC();

	SshPublicKey getHostKey();

	String getKeyExchangeInUse();

	String getHostKeyInUse();

	String getCipherInUseCS();

	String getCipherInUseSC();

	String getMacInUseCS();

	String getMacInUseSC();

	String getCompressionInUseCS();

	String getCompressionInUseSC();

	void sendGlobalRequest(GlobalRequest request, boolean wantReply);

	AbstractRequestFuture getAuthenticatedFuture();

	AbstractRequestFuture getDisconnectFuture();
	
	void removeProperty(String string);

	int getSessionCount();

	long getTotalBytesIn();
	
	long getTotalBytesOut();

	String getRemoteIPAddress();

}
