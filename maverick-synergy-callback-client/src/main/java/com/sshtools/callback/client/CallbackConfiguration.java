package com.sshtools.callback.client;

/*-
 * #%L
 * Callback Client API
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

import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public class CallbackConfiguration {

	public static final String DEFAULT_CALLBACK_ID = "CallbackClient";
	
	String agentName;
	String serverHost;
	int serverPort = 22;
	String remoteUUID;
	String localUUID;
	Long reconnectIntervalMs;
	SshKeyPair privateKey;
	SshPublicKey publicKey;
	String memo;
	String callbackIdentifier = DEFAULT_CALLBACK_ID;
	boolean reconnect = true;
	
	Map<String,Object> properties = new HashMap<>();
	
	public CallbackConfiguration(String agentName, 
			String serverHost, 
			int serverPort, 
			Long reconnectIntervalMs, 
			SshKeyPair privateKey,
			SshPublicKey publicKey, 
			String memo) {
		super();
		this.agentName = agentName;
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.memo = memo;
	}
	
	protected CallbackConfiguration() {
		
	}
	
	public CallbackConfiguration setProperty(String name, Object value) {
		properties.put(name, value);
		return this;
	}
	
	public Object getProperty(String name) {
		return properties.get(name);
	}
	
	public String getAgentName() {
		return agentName;
	}
	
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}
	
	public String getServerHost() {
		return serverHost;
	}
	
	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}
	
	public int getServerPort() {
		return serverPort;
	}
	
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public Long getReconnectIntervalMs() {
		return reconnectIntervalMs==null ? 5000L : reconnectIntervalMs;
	}

	public void setReconnectIntervalMs(Long reconnectIntervalMs) {
		this.reconnectIntervalMs = reconnectIntervalMs;
	}

	public SshKeyPair getPrivateKey() {
		return privateKey;
	}

	public SshPublicKey getPublicKey() {
		return publicKey;
	}

	public String getMemo() {
		return memo;
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}

	public String getCallbackIdentifier() {
		return callbackIdentifier;
	}

	public void setCallbackIdentifier(String callbackIdentifier) {
		this.callbackIdentifier = callbackIdentifier;
	}

	public boolean isReconnect() {
		return reconnect;
	}

	public void setReconnect(boolean reconnect) {
		this.reconnect = reconnect;
	}
	
	
}
