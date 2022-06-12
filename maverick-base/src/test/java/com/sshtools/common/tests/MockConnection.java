/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.GlobalRequest;
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

	@Override
	public void sendGlobalRequest(GlobalRequest request, boolean wantReply) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AbstractRequestFuture getAuthenticatedFuture() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeProperty(String string) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AbstractRequestFuture getDisconnectFuture() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSessionCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalBytesIn() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalBytesOut() {
		// TODO Auto-generated method stub
		return 0;
	}
	

}
