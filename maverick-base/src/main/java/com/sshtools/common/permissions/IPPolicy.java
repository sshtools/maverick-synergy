/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.permissions;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import com.sshtools.common.logger.Log;
import com.sshtools.common.net.CIDRNetwork;
import com.sshtools.common.util.ExpiringConcurrentHashMap;

public class IPPolicy extends Permissions {

	static final int ALLOW_CONNECT = 0x01;
	static final int DISABLE_BAN = 0x02;
	
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
	
	public void disableTemporaryBanning() {
		add(DISABLE_BAN);
	}
	
	public void enableTemporaryBanning() {
		remove(DISABLE_BAN);
	}
	
	public long getTemporaryBanTime() {
		return temporaryBans.getExpiryTime();
	}
	
	protected boolean assertConnection(InetAddress remoteAddress, InetAddress localAddress) {
		
		if(check(ALLOW_CONNECT)) {
			if(check(DISABLE_BAN)) {
				return true;
			}
			return assertAllowed(remoteAddress, localAddress);
		}
		return false;
	}
	
	protected boolean assertAllowed(InetAddress remoteAddress, InetAddress localAddress) {

		try {
			boolean allowed = true;
			
			String addr;
			
			Boolean temporarilyBanned = temporaryBans.getOrDefault(remoteAddress, false);
			if(temporarilyBanned) {
				Log.info("Rejecting IP {} because of temporary ban", remoteAddress.getHostAddress());
				return false;
			}
			addr = remoteAddress.getHostAddress();
			
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
	
	public void flagAddress(String addr) {
		try {
			flagAddress(InetAddress.getByName(addr));
		} catch (UnknownHostException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
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
	
	public final boolean checkConnection(InetAddress remoteAddress, InetAddress localAddress) {
		return assertConnection(remoteAddress, localAddress);
	}
	
	public final boolean checkConnection(String remoteAddress, String localAddress) {
		try {
			return assertConnection(InetAddress.getByAddress(convertAddress(remoteAddress)),
					InetAddress.getByAddress(convertAddress(remoteAddress)));
		} catch (UnknownHostException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	private byte[] convertAddress(String str) {
		byte[] ret = new byte[4];
		String[] s = str.split("\\.");
		for (int i = 0; i < ret.length; i++) {
		    ret[i] = (byte) Integer.parseInt(s[i], 10);
		}
		return ret;
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
