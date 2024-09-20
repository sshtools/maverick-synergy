package com.sshtools.synergy.nio;

/*-
 * #%L
 * Common API
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

import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.synergy.ssh.Connection;

public class ConnectRequestFuture extends AbstractRequestFuture {

	ProtocolEngine transport;
	Connection<?> con;
	String host;
	int port;
	Throwable exception;
	
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
	
	public Throwable getLastError() {
		return exception;
	}

	public void setLastError(Throwable exception) {
		this.exception = exception;
	}
}
