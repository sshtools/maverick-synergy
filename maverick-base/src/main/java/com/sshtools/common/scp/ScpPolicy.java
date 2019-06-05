package com.sshtools.common.scp;

import com.sshtools.common.permissions.Permissions;

public class ScpPolicy extends Permissions {

	boolean scpReadWriteEvents;
	String scpCharsetEncoding = "UTF-8";
	
	public boolean isSCPReadWriteEvents() {
		return scpReadWriteEvents;
	}

	public void setSCPReadWriteEvents(boolean scpReadWriteEvents) {
		this.scpReadWriteEvents = scpReadWriteEvents;
	}

	public String getSCPCharsetEncoding() {
		return scpCharsetEncoding;
	}

	public void setSCPCharsetEncoding(String scpCharsetEncoding) {
		this.scpCharsetEncoding = scpCharsetEncoding;
	}
}
