package com.sshtools.common.ssh;

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
