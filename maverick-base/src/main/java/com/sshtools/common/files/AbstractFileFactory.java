
package com.sshtools.common.files;

import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.permissions.PermissionDeniedException;

public interface AbstractFileFactory<T extends AbstractFile> {

	T getFile(String path) throws PermissionDeniedException, IOException;
	
	Event populateEvent(Event evt);

	T getDefaultPath() throws PermissionDeniedException, IOException;

}
