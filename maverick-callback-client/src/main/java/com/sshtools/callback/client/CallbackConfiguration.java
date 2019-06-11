package com.sshtools.callback.client;

import java.util.Set;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public abstract class CallbackConfiguration {

	String agentName;
	String serverHost;
	int serverPort = 22;
	String remoteUUID;
	String localUUID;
	Long reconnectIntervalMs;
	Set<SshKeyPair> hostKeys;
	Set<SshPublicKey> authorizedKeys;
	
	protected CallbackConfiguration(String agentName, 
			String serverHost, 
			int serverPort, 
			Long reconnectIntervalMs, 
			String remoteUUID, 
			String localUUID, 
			Set<SshKeyPair> hostKeys,
			Set<SshPublicKey> authorizedKeys) {
		super();
		this.agentName = agentName;
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		this.remoteUUID = remoteUUID;
		this.localUUID = localUUID;
		this.authorizedKeys = authorizedKeys;
		this.hostKeys = hostKeys;
	}
	
	protected CallbackConfiguration() {
		
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
	
	public String getRemoteUUID() {
		return remoteUUID;
	}

	public void setRemoteUUID(String remoteUUID) {
		this.remoteUUID = remoteUUID;
	}

	public String getLocalUUID() {
		return localUUID;
	}

	public void setLocalUUID(String localUUID) {
		this.localUUID = localUUID;
	}

	public Set<SshPublicKey> getAuthorizedKeys() {
		return authorizedKeys;
	}

	public void setAuthorizedKeys(Set<SshPublicKey> authorizedKeys) {
		this.authorizedKeys = authorizedKeys;
	}

	public Set<SshKeyPair> getHostKeys() {
		return hostKeys;
	}

	public void setHostKeys(Set<SshKeyPair> hostKeys) {
		this.hostKeys = hostKeys;
	}

	public Long getReconnectIntervalMs() {
		return reconnectIntervalMs==null ? 5000L : reconnectIntervalMs;
	}

	public void setReconnectIntervalMs(Long reconnectIntervalMs) {
		this.reconnectIntervalMs = reconnectIntervalMs;
	}
	
	
}
