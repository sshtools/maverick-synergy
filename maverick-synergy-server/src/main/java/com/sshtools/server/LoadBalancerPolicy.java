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
import java.util.HashSet;
import java.util.Set;

import com.sshtools.common.permissions.IPPolicy;

public class LoadBalancerPolicy {

	boolean proxyProtocolEnabled = false;

	boolean restrictedAccess = true;
	
	Set<String> supportedIPAddresses = new HashSet<>();
	
	IPPolicy ipPolicy = new IPPolicy();
	
	public boolean isProxyProtocolEnabled() {
		return proxyProtocolEnabled;
	}

	public void setProxyProtocolEnabled(boolean proxyProtocolEnabled) {
		this.proxyProtocolEnabled = proxyProtocolEnabled;
	}
	
	public void allowIPAddress(String... remoteAddress) {
		supportedIPAddresses.addAll(Arrays.asList(remoteAddress));
	}

	public boolean isSupportedIPAddress(String remoteAddress) {
		return supportedIPAddresses.contains(remoteAddress);
	}

	public boolean isRestrictedAccess() {
		return restrictedAccess;
	}

	public void setRestrictedAccess(boolean restrictedAccess) {
		this.restrictedAccess = restrictedAccess;
	}

	public IPPolicy getIPPolicy() {
		return ipPolicy;
	}

	public void setIPPolicy(IPPolicy iPPolicy) {
		this.ipPolicy = iPPolicy;
	}
}
