package com.sshtools.common.files.vfs;

import java.io.IOException;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;


public interface VirtualFile extends AbstractFile {

	VirtualMount getMount();
	
	boolean isMount();

	AbstractFile resolveFile() throws PermissionDeniedException, IOException;
}
