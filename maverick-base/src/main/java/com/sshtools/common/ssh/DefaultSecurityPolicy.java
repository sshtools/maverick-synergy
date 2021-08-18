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

package com.sshtools.common.ssh;

import com.sshtools.common.logger.Log;
import com.sshtools.common.util.Utils;

public class DefaultSecurityPolicy implements SecurityPolicy {

	SecurityLevel minimumSecurity;
	boolean managedSecurity;
	
	public DefaultSecurityPolicy(SecurityLevel minimumSecurity, boolean managedSecurity) {
		this.minimumSecurity = minimumSecurity;
		this.managedSecurity = managedSecurity;
	}
	
	@Override
	public SecurityLevel getMinimumSecurityLevel() {
		return minimumSecurity;
	}
	
	@Override
	public boolean isManagedSecurity() {
		return managedSecurity;
	}
	
	@Override
	public boolean isDropSecurityAsLastResort() {
		return false;
	}
	
	@Override
	public void onIncompatibleSecurity(String host, int port, String remoteIdentification, IncompatibleAlgorithm... reports) {
		
		Log.error("Connection to {}:{} could not be established due to incompatible security protocols", host, port);
		Log.error("The remote host identified itself as {}", remoteIdentification);
		Log.error("The following algorithms could not be negotiated:");
		for(IncompatibleAlgorithm report : reports) {
			Log.error("{} could not be negotiated from remote algorithms {}", report.getType().name(), Utils.csv(report.getRemoteAlgorithms()));
		}
	}
}
