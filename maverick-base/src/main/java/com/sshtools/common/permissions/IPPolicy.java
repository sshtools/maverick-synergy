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
package com.sshtools.common.permissions;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.logger.Log;

import com.sshtools.common.net.CIDRNetwork;

public class IPPolicy extends Permissions {

	
	
	static final int ALLOW_CONNECT = 0x01;
	
	List<CIDRNetwork> blacklist = new ArrayList<CIDRNetwork>();
	List<CIDRNetwork> whitelist = new ArrayList<CIDRNetwork>();
	
	public IPPolicy() {
		add(ALLOW_CONNECT);
	}
	
	
	protected boolean assertConnection(SocketAddress remoteAddress, SocketAddress localAddress) {
		if(check(ALLOW_CONNECT)) {
			return assertAllowed(remoteAddress, localAddress);
		}
		return false;
	}
	
	protected boolean assertAllowed(SocketAddress remoteAddress, SocketAddress localAddress) {

		try {
			boolean allowed = true;
			
			String addr;
			InetAddress resolved = ((InetSocketAddress)remoteAddress).getAddress();
			if(resolved==null) {
				addr = ((InetSocketAddress)remoteAddress).getHostString();
			} else {
				addr = resolved.getHostAddress();
			}
			
			if(!whitelist.isEmpty()) {
				allowed = isListed(addr, whitelist);
			}
			
			boolean rejected = isListed(addr, blacklist);
			
			if(Log.isTraceEnabled()) {
				Log.trace("%s is %s by IP policy", remoteAddress.toString(), (allowed && !rejected) ? "allowed" : "denied");
			}
			
			return allowed && !rejected;
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Invalid IP range");
		}
	}

	protected boolean isListed(String addr, List<CIDRNetwork> values) throws UnknownHostException {
		for(CIDRNetwork value : values) {
			if(value.isValidAddressForNetwork(addr)) {
				return true;
			}
		}
		return false;
	}
	
	public final boolean checkConnection(SocketAddress remoteAddress, SocketAddress localAddress) {
		return assertConnection(remoteAddress, localAddress);
	}
	
	public void stopAcceptingConnections() {
		if(Log.isInfoEnabled()) {
			Log.info("Stop accepting connections on IP Policy");
		}
		remove(ALLOW_CONNECT);
	}
	
	public void startAcceptingConnections() {
		if(Log.isInfoEnabled()) {
			Log.info("Start accepting connections on IP Policy");
		}
		add(ALLOW_CONNECT);
	}
	
	public void blacklist(String addr) throws UnknownHostException {
		blacklist.add(new CIDRNetwork(addr));
	}
	
	public void whitelist(String addr) throws UnknownHostException {
		whitelist.add(new CIDRNetwork(addr));
	}
}
