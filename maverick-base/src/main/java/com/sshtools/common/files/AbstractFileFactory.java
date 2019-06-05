package com.sshtools.common.files;

import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public interface AbstractFileFactory<T extends AbstractFile> {

	T getFile(String path, SshConnection con) throws PermissionDeniedException, IOException;
	
	Event populateEvent(Event evt);

	T getDefaultPath(SshConnection con) throws PermissionDeniedException, IOException;

}
