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
package com.sshtools.common.permissions;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sshtools.common.logger.Log;
import com.sshtools.common.net.CIDRNetwork;
import com.sshtools.common.util.ExpiringConcurrentHashMap;

public class IPPolicy extends Permissions {

	static final int ALLOW_CONNECT = 0x01;
	
	IPStore blacklist = new IPStore();
	IPStore whitelist = new IPStore();
	
	ExpiringConcurrentHashMap<InetAddress, Integer> flaggedAddressCounts;
	int failedAuthenticationThreshold = 15;
	ExpiringConcurrentHashMap<InetAddress, Boolean> temporaryBans = new ExpiringConcurrentHashMap<InetAddress, Boolean>(TimeUnit.HOURS.toMillis(5));
	
	public IPPolicy() {
		add(ALLOW_CONNECT);
		setFailedAuthenticationThresholdPeriod(5, TimeUnit.MINUTES);
	}
	
	public void setFailedAuthenticationCountThreshold(int failedAuthenticationThreshold) {
		this.failedAuthenticationThreshold = failedAuthenticationThreshold;
	}
	
	public void setFailedAuthenticationThresholdPeriod(long failedAuthenticationThresholdPeriod, TimeUnit timeUnit) {
		flaggedAddressCounts = new ExpiringConcurrentHashMap<InetAddress, Integer>(timeUnit.toMillis(failedAuthenticationThresholdPeriod));
	}
	
	public void setTemporaryBanTime(long minutes) {
		if(minutes <= 0) {
			throw new IllegalArgumentException("Temporary ban period must be more than zero");
		}
		ExpiringConcurrentHashMap<InetAddress, Boolean> temporaryBans = new ExpiringConcurrentHashMap<InetAddress, Boolean>(TimeUnit.MINUTES.toMillis(minutes));
		temporaryBans.putAll(this.temporaryBans);
		this.temporaryBans = temporaryBans;
	}
	
	public long getTemporaryBanTime() {
		return temporaryBans.getExpiryTime();
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
			
			Boolean temporarilyBanned = temporaryBans.getOrDefault(resolved, false);
			if(temporarilyBanned) {
				Log.info("Rejecting IP {} because of temporary ban", resolved.getHostAddress());
				return false;
			}
			addr = resolved.getHostAddress();
			
			if(!whitelist.isEmpty()) {
				allowed = isListed(addr, whitelist);
			}
			
			boolean rejected = isListed(addr, blacklist);
			
			if(Log.isTraceEnabled()) {
				Log.trace("{} is {} by IP policy", remoteAddress.toString(), (allowed && !rejected) ? "allowed" : "denied");
			}
			
			return allowed && !rejected;
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Invalid IP range");
		}
	}

	protected boolean isListed(String addr, IPStore store) throws UnknownHostException {
		for(CIDRNetwork value : store.getIPs()) {
			if(value.isValidAddressForNetwork(addr)) {
				return true;
			}
		}
		return false;
	}
	
	public void flagAddress(InetAddress addr) {
		
		Integer count = flaggedAddressCounts.getOrDefault(addr, 0);
		
		if(count >= failedAuthenticationThreshold) {
			Log.info("Temporarily banning IP address {} due to failed authentication count of {}", 
					addr.getHostAddress(), count);
			temporaryBans.put(addr, true);
			return;
		}
		
		++count;
		Log.info("Flagging IP address {} with failed authentication count of {}", addr.getHostAddress(), count);
		flaggedAddressCounts.put(addr, count);
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
		Log.info("Blacklisting IP address {}", addr);
		blacklist.add(addr);
	}
	
	public void whitelist(String addr) throws UnknownHostException {
		Log.info("Whitelisting IP address {}", addr);
		whitelist.add(addr);
	}

	public IPStore getBlacklist() {
		return blacklist;
	}

	public void setBlacklist(IPStore blacklist) {
		this.blacklist = blacklist;
	}

	public IPStore getWhitelist() {
		return whitelist;
	}

	public void setWhitelist(IPStore whitelist) {
		this.whitelist = whitelist;
	}

}
