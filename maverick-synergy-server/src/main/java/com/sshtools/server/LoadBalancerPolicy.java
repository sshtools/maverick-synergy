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
package com.sshtools.server;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LoadBalancerPolicy {

	boolean proxyProtocolEnabled = false;

	boolean restrictedAccess = true;
	
	Set<String> supportedIPAddresses = new HashSet<>();
	
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
	
	

}
