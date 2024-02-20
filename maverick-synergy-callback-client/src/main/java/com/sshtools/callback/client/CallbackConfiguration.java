package com.sshtools.callback.client;

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
