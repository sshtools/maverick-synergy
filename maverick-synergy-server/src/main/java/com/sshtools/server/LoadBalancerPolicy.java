package com.sshtools.server;

/*-
 * #%L
 * Server API
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.sshtools.common.permissions.IPPolicy;
import com.sshtools.common.permissions.IPPolicy.IPPolicyBuilder;
import com.sshtools.common.permissions.Policy;

/* TODO Make final at 3.3.0 */
public class LoadBalancerPolicy implements Policy {
	
	/**
	 * Build a new {@link LoadBalancerPolicy}.
	 */
	public final static class LoadBalancerPolicyBuilder {

		private boolean proxyProtocolEnabled = false;
		private boolean restrictedAccess = true;
		private Set<String> supportedIPAddresses = new HashSet<>();
		private Optional<IPPolicy> ipPolicy = Optional.empty();
		private Optional<Predicate<String>> addressChecker = Optional.empty();
		
		private LoadBalancerPolicyBuilder() { }
		
		/**
		 * Create a new {@link LoadBalancerPolicyBuilder} that will be used to configure
		 * and create an {@link LoadBalancerPolicy}.
		 * 
		 * @return builder
		 */
		public static LoadBalancerPolicyBuilder create() {
			return new LoadBalancerPolicyBuilder(); 
		}
		
		/**
		 * Enable the proxy protocol.
		 * 
		 * @return this for chaining
		 */
		public LoadBalancerPolicyBuilder withProxyProtocol() {
			return withProxyProtocol(true);
		}
		
		/**
		 * Set whether the proxy protocol is enabled or not.
		 * 
		 * @param proxyProtocolEnabled enable proxy protocol
		 * @return this for chaining
		 */
		public LoadBalancerPolicyBuilder withProxyProtocol(boolean proxyProtocolEnabled) {
			this.proxyProtocolEnabled = proxyProtocolEnabled;
			return this;
		}
		
		/**
		 * Enable restricted access by default.
		 * 
		 * @return this for chaining
		 */
		public LoadBalancerPolicyBuilder withRestrictedAccess() {
			return withRestrictedAccess(true);
		}
		
		/**
		 * Set whether the restricted access by default is enabled or not.
		 * 
		 * @param restrictedAccess restricted access
		 * @return this for chaining
		 */
		public LoadBalancerPolicyBuilder withRestrictedAccess(boolean restrictedAccess) {
			this.restrictedAccess = restrictedAccess;
			return this;
		}
		
		/**
		 * Set the  associated {@link IPPolicy}.
		 * 
		 * @param ipPolicy IP policy
		 * @return this for chaining
		 */
		public LoadBalancerPolicyBuilder withIPPolicy(IPPolicy ipPolicy) {
			this.ipPolicy = Optional.of(ipPolicy);
			return this;
		}
		
		/**
		 * Set the list of supported IP addresses.
		 * 
		 * @param ipAddress supported addresses
		 * @return this for chaining
		 */
		public LoadBalancerPolicyBuilder withSupportedIPAddresses(String... supportedIPAddresses) {
			return withSupportedIPAddresses(Arrays.asList(supportedIPAddresses));
		} 
		
		/**
		 * Set the list of supported IP addresses.
		 * 
		 * @param ipAddress supported addresses
		 * @return this for chaining
		 */
		public LoadBalancerPolicyBuilder withSupportedIPAddresses(Collection<String> supportedIPAddresses) {
			this.supportedIPAddresses.clear();
			return addSupportedIPAddresses(supportedIPAddresses);
		}
		
		/**
		 * Add to the list of supported IP addresses.
		 * 
		 * @param ipAddress supported addresses
		 * @return this for chaining
		 */
		public LoadBalancerPolicyBuilder addSupportedIPAddresses(String... supportedIPAddresses) {
			return addSupportedIPAddresses(Arrays.asList(supportedIPAddresses));
		}
		
		/**
		 * Add to the list of supported IP addresses.
		 * 
		 * @param ipAddress supported addresses
		 * @return this for chaining
		 */
		public LoadBalancerPolicyBuilder addSupportedIPAddresses(Collection<String> supportedIPAddresses) {
			this.supportedIPAddresses.addAll(supportedIPAddresses);
			return this;
		}
		
		/**
		 * Instead of supplying a list of allowed addresses up front, you can provide you own
		 * lookup mechanism that is called by {@link LoadBalancerPolicy#isSupportedIPAddress} to
		 * check whether an address is supported. 
		 * 
		 * @param addressChecker address checker
		 * @return this for chaining
		 */
		public LoadBalancerPolicyBuilder withAddressChecker(Predicate<String> addressChecker) {
			this.addressChecker = Optional.of(addressChecker);
			return this;
		}
		
		/**
		 * Build a new {@link LoadBalancerPolicy} given the configuration of this builder.
		 * 
		 * @return policy
		 */
		public LoadBalancerPolicy build() {
			return new LoadBalancerPolicy(this);
		}
	}
	
	/* TODO make all of these final, remove all deprecated setters at 3.3.x+ */
	private boolean proxyProtocolEnabled;
	private boolean restrictedAccess;
	private Set<String> supportedIPAddresses;
	private IPPolicy ipPolicy;
	private final Predicate<String> addressChecker;
	
	@Deprecated(since = "3.2.0", forRemoval = true)
	public LoadBalancerPolicy() {
		proxyProtocolEnabled = false;
		restrictedAccess = true;
		supportedIPAddresses = new HashSet<>();
		ipPolicy = new IPPolicy();
		addressChecker = addr -> supportedIPAddresses.contains(addr);
	}
	
	private LoadBalancerPolicy(LoadBalancerPolicyBuilder bldr) {
		this.proxyProtocolEnabled = bldr.proxyProtocolEnabled;
		this.restrictedAccess = bldr.restrictedAccess;
		this.supportedIPAddresses = Collections.unmodifiableSet(new HashSet<>(bldr.supportedIPAddresses));
		this.ipPolicy = bldr.ipPolicy.orElseGet(() -> IPPolicyBuilder.create().build());
		this.addressChecker = bldr.addressChecker.orElseGet(() -> addr -> supportedIPAddresses.contains(addr));
	}
	
	public boolean isProxyProtocolEnabled() {
		return proxyProtocolEnabled;
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setProxyProtocolEnabled(boolean proxyProtocolEnabled) {
		this.proxyProtocolEnabled = proxyProtocolEnabled;
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public void allowIPAddress(String... remoteAddress) {
		supportedIPAddresses.addAll(Arrays.asList(remoteAddress));
	}

	public boolean isSupportedIPAddress(String remoteAddress) {
		return addressChecker.test(remoteAddress);
	}

	public boolean isRestrictedAccess() {
		return restrictedAccess;
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setRestrictedAccess(boolean restrictedAccess) {
		this.restrictedAccess = restrictedAccess;
	}

	public IPPolicy getIPPolicy() {
		return ipPolicy;
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setIPPolicy(IPPolicy iPPolicy) {
		this.ipPolicy = iPPolicy;
	}

	@Override
	public Class<? extends Policy> type() {
		return LoadBalancerPolicy.class;
	}
}
