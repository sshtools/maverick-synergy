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
