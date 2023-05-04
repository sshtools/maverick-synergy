package com.sshtools.server;

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
