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
