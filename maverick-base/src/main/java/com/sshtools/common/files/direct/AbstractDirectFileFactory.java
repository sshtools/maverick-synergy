package com.sshtools.common.files.direct;

import java.io.File;
import java.io.IOException;

import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;

public abstract class AbstractDirectFileFactory<T extends AbstractDirectFile<T>> implements AbstractFileFactory<T> {

	File homeDirectory;;
	
	public AbstractDirectFileFactory() {
	}
	
	public AbstractDirectFileFactory(File homeDirectory) {
		this.homeDirectory = homeDirectory;
	}
	
	public T getDefaultPath() throws PermissionDeniedException, IOException {
		return getFile("");
	}
}
