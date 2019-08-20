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
import com.sshtools.common.files.AbstractFileHomeFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public class VFSFileFactory implements AbstractFileFactory<VFSFile> {
	
	private FileSystemOptions opts;
	private FileObject defaultPath;
	private AbstractFileHomeFactory homeFactory;
	private boolean useURI = true;
	private FileSystemManager manager;

	public VFSFileFactory() throws FileNotFoundException {
		this((FileSystemManager) null);
	}

	public VFSFileFactory(AbstractFileHomeFactory homeFactory) throws FileNotFoundException {
		this((FileSystemManager) null, homeFactory);
	}

	public VFSFileFactory(FileSystemOptions opts) throws FileNotFoundException {
		this((FileSystemManager) null, opts, null);
	}

	public VFSFileFactory(FileSystemOptions opts, AbstractFileHomeFactory homeFactory) throws FileNotFoundException {
		this((FileSystemManager) null, opts, homeFactory);
	}

	public VFSFileFactory(FileSystemManager manager) throws FileNotFoundException {
		this(manager, new FileSystemOptions(), null);
	}

	public VFSFileFactory(FileSystemManager manager, AbstractFileHomeFactory homeFactory) throws FileNotFoundException {
		this(manager, new FileSystemOptions(), homeFactory);
	}

	public VFSFileFactory(FileSystemManager manager, FileSystemOptions opts) throws FileNotFoundException {
		this(manager, opts, null);
	}

	public VFSFileFactory(FileSystemManager manager, FileSystemOptions opts, AbstractFileHomeFactory homeFactory)
			throws FileNotFoundException {
		this.opts = opts;
		this.homeFactory = homeFactory;
		try {
			if (manager == null) {
				manager = VFS.getManager();
			}
		} catch (FileSystemException fse) {
			throw new FileNotFoundException("Could not obtain VFS manager.");
		}
		this.manager = manager;
		if (homeFactory == null) {
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

	public VFSFile getFile(String parent, String path, SshConnection con) throws PermissionDeniedException, IOException {
		FileObject obj;
		try {
			obj = manager.resolveFile(parent, opts);
			obj = obj.resolveFile(path);
			return new VFSFile(obj, this, con);
		} catch (FileSystemException e) {
			try {
				FileObject alt;
				if (homeFactory == null) {
					alt = manager.resolveFile(defaultPath, parent);
				} else {
					alt = manager.resolveFile(manager.resolveFile(homeFactory.getHomeDirectory(con)), parent);
				}
				alt = alt.resolveFile(path);
				return new VFSFile(alt, this, con);
			} catch (Exception e1) {
				if(Log.isDebugEnabled()) {
					Log.debug("Unable to resolve file " + path, e1);
				}
				throw new FileNotFoundException("Path " + path + " was not found");
			}
		}
	}

	public VFSFile getFile(String path, SshConnection con) throws PermissionDeniedException, IOException {
		FileObject obj;
		try {
			obj = manager.resolveFile(path, opts);
		} catch (FileSystemException e) {
			try {
				if (homeFactory == null) {
					obj = manager.resolveFile(defaultPath, path);
				} else {
					obj = manager.resolveFile(manager.resolveFile(homeFactory.getHomeDirectory(con)), path);
				}
			} catch (Exception e1) {
				if(Log.isDebugEnabled()) {
					Log.debug("Unable to resolve file " + path, e1);
				}
				throw new FileNotFoundException("Path " + path + " was not found");
			}
		}
		return new VFSFile(obj, this, con);
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

	public VFSFile getDefaultPath(SshConnection con) throws PermissionDeniedException, IOException {
		return getFile("", con);
	}
}
