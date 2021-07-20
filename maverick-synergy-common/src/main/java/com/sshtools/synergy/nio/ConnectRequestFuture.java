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

package com.sshtools.synergy.nio;

import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.synergy.ssh.Connection;

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
