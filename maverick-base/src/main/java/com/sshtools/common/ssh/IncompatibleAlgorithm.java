package com.sshtools.common.ssh;

public class IncompatibleAlgorithm {

	public enum ComponentType { CIPHER_CS, CIPHER_SC, MAC_CS, MAC_SC, KEYEXCHANGE, PUBLICKEY, COMPRESSION_CS, COMPRESSION_SC };

	ComponentType type;
	String[] localAlgorithms;
	String[] remoteAlgorithms;
	
	public IncompatibleAlgorithm(ComponentType type, String[] localAlgorithms, String[] remoteAlgorithms) {
		this.type = type;
		this.localAlgorithms = localAlgorithms;
		this.remoteAlgorithms = remoteAlgorithms;
	}

	public ComponentType getType() {
		return type;
	}

	public String[] getLocalAlgorithms() {
		return localAlgorithms;
	}

	public String[] getRemoteAlgorithms() {
		return remoteAlgorithms;
	}
	
	
}
