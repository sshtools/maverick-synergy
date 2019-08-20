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
