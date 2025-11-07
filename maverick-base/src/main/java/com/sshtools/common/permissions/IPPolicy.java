package com.sshtools.common.permissions;

/*-
 * #%L
 * Base API
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.sshtools.common.logger.Log;
import com.sshtools.common.net.CIDRNetwork;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.shell.ShellPolicy.ShellPolicyBuilder;
import com.sshtools.common.util.ExpiringConcurrentHashMap;

/**
 * Represents various IP related policy.
 * 
 * Note, will be made <code>final</code> at version 3.3.0, and will only be able to
 * be constructed using the {@link ShellPolicyBuilder}.
 * 
 * TODO make final at 3.3.0+
 */
public class IPPolicy extends Permissions {
	
	/**
	 * IP permissions
	 */
	public enum IPPermission implements Permission {
		ALLOW_CONNECT,
		DISABLE_BAN;

		@Override
		public int nativeMask() {
			switch(this) {
			case ALLOW_CONNECT:
				return IPPolicy.ALLOW_CONNECT;
			default:
				return IPPolicy.DISABLE_BAN;
			}
		}
	}

	/**
	 * @deprecated See {@link IPPermission#ALLOW_CONNECT}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public static final int ALLOW_CONNECT = 0x01;

	/**
	 * @deprecated See {@link IPPermission#ALLOW_CONNECT}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public static final int DISABLE_BAN = 0x02;

	
	/**
	 * Build a new {@link IPPolicy}.
	 */
	public final static class IPPolicyBuilder extends AbstractPermissionBuilder<IPPermission, IPPolicyBuilder> {

		private int failedAuthenticationThreshold = 15;
		private Duration failedAuthenticationThresholdPeriod = Duration.ofMinutes(5);
		private Duration temporaryBanTime = Duration.ofHours(5);
		
		private IPPolicyBuilder() { }
		
		/**
		 * Create a new {@link IPPolicyBuilder} that will be used to configure
		 * and create an {@link IPPolicy}.
		 * 
		 * @return builder
		 */
		public static IPPolicyBuilder create() {
			return new IPPolicyBuilder(); 
		}
		/**
		 * Set the maximum number of failed authentication attempts before an address is
		 * temporarily banned.
		 * 
		 *  @param failedAuthenticationThreshold failed authentication attempts
		 *  @return this for chaining
		 */
		public IPPolicyBuilder withFailedAuthenticationThreshold(int failedAuthenticationThreshold) {
			this.failedAuthenticationThreshold = failedAuthenticationThreshold;
			return this;
		}
		
		/**
		 * Set the amount of time in milliseconds failed authentication attempts are remembered.
		 * 
		 *  @param failedAuthenticationThresholdPeriod failed authentication threshold period in milliseconds 
		 *  @return this for chaining
		 */
		public IPPolicyBuilder withFailedAuthenticationThresholdPeriod(long failedAuthenticationThresholdPeriod) {
			return withFailedAuthenticationThresholdPeriod(Duration.ofMillis(failedAuthenticationThresholdPeriod));
		}
		
		/**
		 * Set the amount of time failed authentication attempts are remembered.
		 * 
		 *  @param failedAuthenticationThresholdPeriod failed authentication threshold period
		 *  @return this for chaining
		 */
		public IPPolicyBuilder withFailedAuthenticationThresholdPeriod(Duration failedAuthenticationThresholdPeriod) {
			this.failedAuthenticationThresholdPeriod = failedAuthenticationThresholdPeriod;
			return this;
		}
		
		/**
		 * Set the amount of time an address is banned in milliseconds
		 * 
		 *  @param temporaryBanTime temporary ban time in milliseconds 
		 *  @return this for chaining
		 */
		public IPPolicyBuilder withTemporaryBanTime(long temporaryBanTime) {
			return withTemporaryBanTime(Duration.ofMillis(temporaryBanTime));
		}
		
		/**
		 * Set the amount of time an address is banned for when it reaches the failed authentication
		 * attempt threshold.
		 * 
		 *  @param temporaryBanTime temporary ban time
		 *  @return this for chaining
		 */
		public IPPolicyBuilder withTemporaryBanTime(Duration temporaryBanTime) {
			this.temporaryBanTime = temporaryBanTime;
			return this;
		}
		
		/**
		 * Build a new {@link ShellPolicy} given the configuration of this builder.
		 * 
		 * @return policy
		 */
		public IPPolicy build() {
			return new IPPolicy(this);
		}
	}
	/* TODO make all of these private + final, remove all deprecated setters at 3.3.x+ */
	private int failedAuthenticationThreshold;
	private IPStore blacklist = new IPStore();
	private IPStore whitelist = new IPStore();
	
	private ExpiringConcurrentHashMap<InetAddress, Integer> flaggedAddressCounts;
	private ExpiringConcurrentHashMap<InetAddress, Boolean> temporaryBans;

	/**
	 * Construct a new policy
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public IPPolicy() {
		add(ALLOW_CONNECT);
		failedAuthenticationThreshold = 15;
		setFailedAuthenticationThresholdPeriod(5, TimeUnit.MINUTES);
		temporaryBans = new ExpiringConcurrentHashMap<InetAddress, Boolean>(TimeUnit.HOURS.toMillis(5));
	}
	
	private IPPolicy(IPPolicyBuilder bldr) {
		super(bldr);
		failedAuthenticationThreshold = bldr.failedAuthenticationThreshold;
		flaggedAddressCounts = new ExpiringConcurrentHashMap<InetAddress, Integer>(bldr.failedAuthenticationThresholdPeriod.toMillis());
		temporaryBans = new ExpiringConcurrentHashMap<InetAddress, Boolean>(bldr.temporaryBanTime.toMillis());
	}

	/**
	 * Set the maximum number of failed authentication attempts from a particular address before
	 * an address will be temporarily banned. 
	 * 
	 * @param failedAuthenticationThreshold failed authentication threshold
	 * @deprecated will become immutable, use {@link IPPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setFailedAuthenticationCountThreshold(int failedAuthenticationThreshold) {
		this.failedAuthenticationThreshold = failedAuthenticationThreshold;
	}
	
	/**
	 * @deprecated will become immutable, use {@link IPPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setFailedAuthenticationThresholdPeriod(long failedAuthenticationThresholdPeriod, TimeUnit timeUnit) {
		flaggedAddressCounts = new ExpiringConcurrentHashMap<InetAddress, Integer>(timeUnit.toMillis(failedAuthenticationThresholdPeriod));
	}
	
	/**
	 * @deprecated will become immutable, use {@link IPPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
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
		
		if(check(DISABLE_BAN)) {
			return;
		}
		
		Integer count = flaggedAddressCounts.getOrDefault(addr, 0);
		
		if(count >= failedAuthenticationThreshold) {
			if(Log.isInfoEnabled()) {
			Log.info("Temporarily banning IP address {} due to failed authentication count of {}", 
					addr.getHostAddress(), count);
			}
			temporaryBans.put(addr, true);
			return;
		}
		
		++count;
		if(Log.isInfoEnabled()) {
			Log.info("Flagging IP address {} with failed authentication count of {}", addr.getHostAddress(), count);
		}
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
