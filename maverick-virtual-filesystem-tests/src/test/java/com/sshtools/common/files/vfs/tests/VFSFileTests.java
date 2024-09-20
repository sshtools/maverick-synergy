package com.sshtools.common.files.vfs.tests;

/*-
 * #%L
 * Virtual File System Tests
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
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
