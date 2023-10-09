package com.sshtools.common.sftp.extensions;

import com.sshtools.common.sftp.SftpExtension;

public abstract class AbstractSftpExtension implements SftpExtension {

	boolean declaredInVersion;
	String name;

	protected AbstractSftpExtension(String name, boolean declaredInVersion) {
		this.declaredInVersion = declaredInVersion;
		this.name = name;
	}
	
	@Override
	public boolean isDeclaredInVersion() {
		return declaredInVersion;
	}

	@Override
	public byte[] getDefaultData() {
		if(declaredInVersion) {
			return generateDefaultData();
		}
		throw new UnsupportedOperationException();
	}
	
	protected byte[] generateDefaultData() {
		return new byte[0];
	}

	@Override
	public String getName() {
		return name;
	}

}
