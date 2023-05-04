package com.sshtools.common.files.vfs.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.junit.Test;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.direct.DirectFileFactory;
import com.sshtools.common.files.vfs.VFSFileFactory;
import com.sshtools.common.files.vfs.VirtualFile;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMountTemplate;
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

	@Test
	public void testMounts() throws IOException, PermissionDeniedException {
		
		VirtualFileFactory factory = new VirtualFileFactory(
				new VirtualMountTemplate("/home", super.getBaseFolder().getAbsolutePath(), 
				new DirectFileFactory(super.getBaseFolder()), false),
				new VirtualMountTemplate("/", "mem://", new VFSFileFactory(), false),
				new VirtualMountTemplate("/level1/level2", "tmp://", new VFSFileFactory(), false));
		
		VirtualFile homeMount = factory.getFile("/home");
		assertTrue(homeMount.isMount());
		
		VirtualFile rootMount = factory.getFile("/");
		assertTrue(rootMount.isMount());
		assertTrue(rootMount.isReadable());
		assertTrue(rootMount.isWritable());
		
		VirtualFile intermediaryMount = factory.getFile("/level1");
		assertTrue(intermediaryMount.isMount());
		assertTrue(intermediaryMount.isReadable());
		assertFalse(intermediaryMount.isWritable());
		
		
		VirtualFile secondLevelMount = factory.getFile("/level1/level2");
		assertTrue(secondLevelMount.isMount());
		assertTrue(secondLevelMount.isReadable());
		assertTrue(secondLevelMount.isWritable());
	}
}