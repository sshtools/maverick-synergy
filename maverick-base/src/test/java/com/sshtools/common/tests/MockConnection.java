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
package com.sshtools.common.tests;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.events.EventListener;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshConnectionManager;
import com.sshtools.common.ssh.components.SshPublicKey;

public class MockConnection implements SshConnection {

	boolean connected = true;
	String username;
	String sessionId;
	String remoteIdentification = "SSH-2.0-MockConnection";
	InetSocketAddress localAddress;
	InetSocketAddress remoteAddress;
	Map<String,Object> properties = new HashMap<>();
	Context context;
	
	public MockConnection(String username, String sessionId, InetSocketAddress localAddress, InetSocketAddress remoteAddress, Context context) {
		this.username = username;
		this.sessionId = sessionId;
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
		this.context = context;
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
	public Context getContext() {
		return context;
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

	@Override
	public String getRemoteIdentification() {
		return remoteIdentification;
	}

	@Override
	public void startLogging() throws IOException {

	}

	@Override
	public void startLogging(Level trace) throws IOException {
		
	}

	@Override
	public void disconnect() {
		disconnect("By Application");
	}

	@Override
	public void addEventListener(EventListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeEventListener(EventListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTask(ConnectionAwareTask r) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String[] getRemotePublicKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getRemoteKeyExchanges() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getRemoteCompressionsCS() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getRemoteCompressionsSC() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getRemoteCiphersCS() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getRemoteCiphersSC() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getRemoteMacsCS() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getRemoteMacsSC() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SshPublicKey getHostKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKeyExchangeInUse() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHostKeyInUse() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCipherInUseCS() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCipherInUseSC() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMacInUseCS() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMacInUseSC() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCompressionInUseCS() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCompressionInUseSC() {
		// TODO Auto-generated method stub
		return null;
	}
	

}
