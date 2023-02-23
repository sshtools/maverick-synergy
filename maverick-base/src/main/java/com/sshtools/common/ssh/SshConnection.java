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

package com.sshtools.common.ssh;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Locale;

import com.sshtools.common.events.EventListener;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.ssh.components.SshPublicKey;


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
	
	/**
	 * @deprectated use getRemoteIPAddress instead
	 * @return
	 */
	@Deprecated
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
