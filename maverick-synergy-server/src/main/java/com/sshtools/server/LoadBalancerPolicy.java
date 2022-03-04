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
