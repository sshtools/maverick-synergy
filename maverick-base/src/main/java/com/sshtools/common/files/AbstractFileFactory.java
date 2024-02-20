package com.sshtools.common.files;

import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.permissions.PermissionDeniedException;

public interface AbstractFileFactory<T extends AbstractFile> {

	T getFile(String path) throws PermissionDeniedException, IOException;
	
	default Event populateEvent(Event evt) { return evt; }

	default T getDefaultPath() throws PermissionDeniedException, IOException { return getFile(""); }

}
