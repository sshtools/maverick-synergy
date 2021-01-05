/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.files.vfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;

public class VFSFileFactory implements AbstractFileFactory<VFSFile> {
	
	private FileSystemOptions opts;
	private FileObject defaultPath;
	private boolean useURI = true;
	private FileSystemManager manager;
	private String homeDirectory;

	public VFSFileFactory() throws FileNotFoundException {
		this((FileSystemManager) null);
	}
	
	public VFSFileFactory(String homeDirectory) throws FileNotFoundException {
		this((FileSystemManager) null, homeDirectory);
	}

	public VFSFileFactory(FileSystemOptions opts) throws FileNotFoundException {
		this((FileSystemManager) null, opts, null);
	}

	public VFSFileFactory(FileSystemOptions opts, String homeDirectory) throws FileNotFoundException {
		this((FileSystemManager) null, opts, homeDirectory);
	}

	public VFSFileFactory(FileSystemManager manager) throws FileNotFoundException {
		this(manager, new FileSystemOptions(), null);
	}

	public VFSFileFactory(FileSystemManager manager, String homeDirectory) throws FileNotFoundException {
		this(manager, new FileSystemOptions(), homeDirectory);
	}

	public VFSFileFactory(FileSystemManager manager, FileSystemOptions opts) throws FileNotFoundException {
		this(manager, opts, null);
	}

	public VFSFileFactory(FileSystemManager manager, FileSystemOptions opts, String homeDirectory)
			throws FileNotFoundException {
		this.opts = opts;
		this.homeDirectory = homeDirectory;
		try {
			if (manager == null) {
				manager = VFS.getManager();
			}
		} catch (FileSystemException fse) {
			throw new FileNotFoundException("Could not obtain VFS manager.");
		}
		this.manager = manager;
		if (homeDirectory == null) {
			try {
				defaultPath = manager.resolveFile(new File(".").getAbsolutePath());
			} catch (FileSystemException e) {
				if(Log.isDebugEnabled()) {
					Log.debug("Unable to determine default path", e);
				}
				throw new FileNotFoundException(
						"Unable to determine a default path. Pass a AbstractFileHomeFactory instance into this constructor of VFSFileFactory");
			}
		}
	}

	public FileSystemManager getFileSystemManager() {
		return manager;
	}

	public boolean isReturnURIForPath() {
		return useURI;
	}

	public void setReturnURIForPath(boolean useURI) {
		this.useURI = useURI;
	}

	public VFSFile getFile(String parent, String path) throws PermissionDeniedException, IOException {
		FileObject obj;
		try {
			obj = manager.resolveFile(parent, opts);
			obj = obj.resolveFile(path);
			return new VFSFile(obj, this);
		} catch (FileSystemException e) {
			try {
				FileObject alt;
				if (homeDirectory == null) {
					alt = manager.resolveFile(defaultPath, parent);
				} else {
					alt = manager.resolveFile(manager.resolveFile(homeDirectory), parent);
				}
				alt = alt.resolveFile(path);
				return new VFSFile(alt, this);
			} catch (Exception e1) {
				if(Log.isDebugEnabled()) {
					Log.debug("Unable to resolve file " + path, e1);
				}
				throw new FileNotFoundException("Path " + path + " was not found");
			}
		}
	}

	public VFSFile getFile(String path) throws PermissionDeniedException, IOException {
		FileObject obj;
		try {
			obj = manager.resolveFile(path, opts);
		} catch (FileSystemException e) {
			try {
				if (homeDirectory == null) {
					obj = manager.resolveFile(defaultPath, path);
				} else {
					obj = manager.resolveFile(manager.resolveFile(homeDirectory), path);
				}
			} catch (Exception e1) {
				if(Log.isDebugEnabled()) {
					Log.debug("Unable to resolve file " + path, e1);
				}
				throw new FileNotFoundException("Path " + path + " was not found");
			}
		}
		return new VFSFile(obj, this);
	}

	public Event populateEvent(Event evt) {
		return evt;
	}

	public void setDefaultPath(String path) throws FileNotFoundException {
		try {
			defaultPath = manager.resolveFile(path, opts);
		} catch (FileSystemException e) {
			if(Log.isDebugEnabled()) {
				Log.debug("Unable to set default path", e);
			}
			throw new FileNotFoundException(
					"Unable to determine a default path. Set AbstractFileHomeFactory instance on VFSFileFactory");
		}
	}

	public VFSFile getDefaultPath() throws PermissionDeniedException, IOException {
		return getFile("");
	}
}
