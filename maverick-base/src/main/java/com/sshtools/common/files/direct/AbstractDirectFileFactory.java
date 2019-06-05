package com.sshtools.common.files.direct;

import java.io.IOException;

import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileHomeFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public abstract class AbstractDirectFileFactory<T extends AbstractDirectFile<T>> implements AbstractFileFactory<T> {

	AbstractFileHomeFactory homeFactory = null;
	
	public AbstractDirectFileFactory() {
	}
	
	public AbstractDirectFileFactory(AbstractFileHomeFactory homeFactory) {
		this.homeFactory = homeFactory;
	}
	
	public T getDefaultPath(SshConnection con) throws PermissionDeniedException, IOException {
		return getFile("", con);
	}
}
