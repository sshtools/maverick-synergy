
package com.sshtools.common.files.vfs;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;

public class VirtualFileWithVFSFileFactoryTests extends VFSFileTests {

	VirtualFileFactory factory; 
	
	protected File getBaseFolder() throws IOException {
		File baseFolder = super.getBaseFolder();
		if(Objects.isNull(factory)) {
			try {
				factory = new VirtualFileFactory(new VirtualMountTemplate("/", baseFolder.getAbsolutePath(), 
						new VFSFileFactory(baseFolder.toURI().toASCIIString()), false));
			} catch (PermissionDeniedException e) {
				throw new IOException(e.getMessage(), e);
			}
		}
		return baseFolder;
 	}
	
	
	@Override
	protected AbstractFile getFile(String path) throws PermissionDeniedException, IOException {
		return factory.getFile(path);
	}



	@Override
	protected String getBasePath() throws IOException {
		return "/";
	}


	@Override
	protected String getCanonicalPath() throws IOException {
		return "/";
	}
	
}