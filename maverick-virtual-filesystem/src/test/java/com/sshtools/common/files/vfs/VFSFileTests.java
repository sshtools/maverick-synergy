
package com.sshtools.common.files.vfs;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.apache.commons.vfs2.VFS;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.tests.DirectFileTests;

public class VFSFileTests extends DirectFileTests {

	VFSFileFactory factory; 
	
	protected File getBaseFolder() throws IOException {
		File baseFolder = super.getBaseFolder();
		if(Objects.isNull(factory)) {
			factory = new VFSFileFactory(VFS.getManager().resolveFile(
					baseFolder.getAbsolutePath()).getName().getURI());
		}
		return baseFolder;
 	}
	
	
	@Override
	protected AbstractFile getFile(String path) throws PermissionDeniedException, IOException {
		return factory.getFile(path);
	}


	@Override
	protected String getBasePath() throws IOException {
		return VFS.getManager().resolveFile(getBaseFolder().getAbsolutePath()).getName().getURI();
	}


	@Override
	protected String getCanonicalPath() throws IOException {
		return VFS.getManager().resolveFile(getBaseFolder().getAbsolutePath()).getName().getURI();
	}

	
}
