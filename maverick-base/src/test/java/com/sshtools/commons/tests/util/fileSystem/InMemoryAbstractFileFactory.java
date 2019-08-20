/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.commons.tests.util.fileSystem;

import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public class InMemoryAbstractFileFactory implements AbstractFileFactory<InMemoryAbstractFile> {
	
	InMemoryFileSystem fs = new InMemoryFileSystem();
	InMemoryFile home = null;
	
	public InMemoryAbstractFileFactory() {
		try {
			boolean isHome = this.fs.exists("/home");
			if (this.home == null && isHome) {
				this.home = this.fs.getFile("/home");
			} else if (this.home == null && !isHome) {
				this.home = this.fs.createFolder(this.fs.root(), "home");
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	} 

	/**
	 * Works on assumption, file will always have extension and directory will not
	 */
	@Override
	public InMemoryAbstractFile getFile(String path, SshConnection con) throws PermissionDeniedException, IOException {
		if (path.startsWith("/")) {
			return createFile(path, con);
		} else {
			String username = con.getUsername();
			String userFolderPath = String.format("/home/%s", username);
			boolean isUserFolder = this.fs.exists(userFolderPath);

			if (!isUserFolder) {
				this.fs.createFolder(this.home, username);
			}
			
			String filePath = userFolderPath + InMemoryFile.getPathSeperator() + path;
			return createFile(filePath, con);
		}
	}
	
	@Override
	public Event populateEvent(Event evt) {
		return null;
	}

	@Override
	public InMemoryAbstractFile getDefaultPath(SshConnection con) throws PermissionDeniedException, IOException {
		return null;
	}
	
	private InMemoryAbstractFile createFile(String path, SshConnection con) throws IOException {
		if (this.fs.exists(path)) {
			return new InMemoryAbstractFile(this.fs.getFile(path), this, con);
		}
		String parent = InMemoryFileSystem.computeParentPath(path);
		String fileName = InMemoryFileSystem.computeFileName(path);
		InMemoryFile parentFileObject = this.fs.getFile(parent);
		InMemoryFile fileObject = null;
		if (fileName.contains(".")) {
			fileObject = this.fs.createFile(parentFileObject, fileName);
		} else {
			fileObject = this.fs.createFolder(parentFileObject, fileName);
		}
		return new InMemoryAbstractFile(fileObject, this, con);
	}

	

}
