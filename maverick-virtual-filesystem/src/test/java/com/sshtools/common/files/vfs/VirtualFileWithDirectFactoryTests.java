
package com.sshtools.common.files.vfs;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.direct.DirectFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.tests.DirectFileTests;

public class VirtualFileWithDirectFactoryTests extends DirectFileTests {

	VirtualFileFactory factory; 
	
	protected File getBaseFolder() throws IOException {
		File baseFolder = super.getBaseFolder();
		if(Objects.isNull(factory)) {
			try {
				factory = new VirtualFileFactory(new VirtualMountTemplate("/", baseFolder.getAbsolutePath(), 
						new DirectFileFactory(baseFolder), false));
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