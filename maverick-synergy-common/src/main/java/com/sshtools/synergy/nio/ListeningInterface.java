/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.synergy.nio;

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