package com.sshtools.common.nio;

import java.net.Inet6Address;
import java.net.InetSocketAddress;

/**
 * Represents a listening interface for incoming socket connections.
 */
public class ListeningInterface {
	private InetSocketAddress addressToBind;
	private ProtocolContextFactory<?> contextFactory;
	private int actualPort;
	private int backlog;
	private boolean reuseAddress = true;
	
	public ListeningInterface(InetSocketAddress addressToBind, ProtocolContextFactory<?> context) {
		this(addressToBind, context, 50);
	}

	public ListeningInterface(InetSocketAddress addressToBind, ProtocolContextFactory<?> context, int backlog) {
		this.addressToBind = addressToBind;
		this.contextFactory = context;
		this.backlog = backlog;
	}

	public int getActualPort() {
		return actualPort;
	}

	public void setActualPort(int actualPort) {
		this.actualPort = actualPort;
	}

	public InetSocketAddress getAddressToBind() {
		return addressToBind;
	}

	public ProtocolContextFactory<?> getContextFactory() {
		return contextFactory;
	}

	public boolean isIPV6Interface() {
		return addressToBind.getAddress() instanceof Inet6Address;
	}

	public int getBacklog() {
		return backlog;
	}

	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}
	
	public boolean getSocketOptionReuseAddress() {
		return reuseAddress;
	}
	
	public void setSocketOptionReuseAddress(boolean reuseAddress) {
		this.reuseAddress = reuseAddress;
	}

}