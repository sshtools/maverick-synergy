package com.sshtools.common.files.direct;

import java.io.File;
import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.permissions.PermissionDeniedException;

/**
 * Deprecated, use {@link NioFile} and {@link NioFileFactory} instead.
 */
@Deprecated(since = "3.1.0", forRemoval = true)
public class DirectFileFactory extends AbstractDirectFileFactory<DirectFile> {

	File defaultPath = new File(".");
	boolean sandbox = false;
	
	public DirectFileFactory(File homeDirectory) {
		super(homeDirectory);
	}
	
	public DirectFileFactory(File homeDirectory, boolean sandbox) {
		super(homeDirectory);
		this.sandbox = sandbox;
	}
	
	public DirectFile getFile(String path)
			throws PermissionDeniedException, IOException {

		DirectFile file =  new DirectFile(path, this, homeDirectory);
		
		if(sandbox) {
			if(!file.getCanonicalPath().startsWith(homeDirectory.getCanonicalPath())) {
				throw new PermissionDeniedException("You cannot access paths other than your home directory");
			}
		}
		return file;
	}

	public Event populateEvent(Event evt) {
		return evt;
	}

}
