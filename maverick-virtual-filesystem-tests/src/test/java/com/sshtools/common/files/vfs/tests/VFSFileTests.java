package com.sshtools.common.files.vfs.tests;
import java.io.IOException;

import org.apache.commons.vfs2.VFS;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.vfs.VFSFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.tests.DirectFileTests;
import com.sshtools.common.util.FileUtils;

public class VFSFileTests extends DirectFileTests {

	VFSFileFactory factory; 
		
	protected void setup() throws IOException {
		factory = new VFSFileFactory(getBaseFolder().getAbsoluteFile().toURI().toASCIIString());
	}
	
	protected void clean() throws IOException {
		FileUtils.deleteFolder(getBaseFolder());
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
