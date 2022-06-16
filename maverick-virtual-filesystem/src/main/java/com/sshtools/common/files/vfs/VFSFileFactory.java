/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.files.vfs;

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
	private String defaultDirectory;

	public VFSFileFactory() throws FileNotFoundException {
		this((FileSystemManager) null);
	}
	
	public VFSFileFactory(String defaultDirectory) throws FileNotFoundException {
		this((FileSystemManager) null, defaultDirectory);
	}

	public VFSFileFactory(FileSystemOptions opts) throws FileNotFoundException {
		this((FileSystemManager) null, opts, null);
	}

	public VFSFileFactory(FileSystemOptions opts, String defaultDirectory) throws FileNotFoundException {
		this((FileSystemManager) null, opts, defaultDirectory);
	}

	public VFSFileFactory(FileSystemManager manager) throws FileNotFoundException {
		this(manager, new FileSystemOptions(), null);
	}

	public VFSFileFactory(FileSystemManager manager, String defaultDirectory) throws FileNotFoundException {
		this(manager, new FileSystemOptions(), defaultDirectory);
	}

	public VFSFileFactory(FileSystemManager manager, FileSystemOptions opts) throws FileNotFoundException {
		this(manager, opts, null);
	}

	public VFSFileFactory(FileSystemManager manager, FileSystemOptions opts, String defaultDirectory)
			throws FileNotFoundException {
		this.opts = opts;
		this.defaultDirectory = defaultDirectory;
		try {
			if (manager == null) {
				manager = VFS.getManager();
			}
		} catch (FileSystemException fse) {
			throw new FileNotFoundException("Could not obtain VFS manager.");
		}
		this.manager = manager;
		if (defaultDirectory == null) {
			try {
				defaultPath = manager.resolveFile(System.getProperty("maverick.vfsDefaultPath", "."));
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
				if (defaultDirectory == null) {
					alt = manager.resolveFile(defaultPath, parent);
				} else {
					alt = manager.resolveFile(manager.resolveFile(defaultDirectory), parent);
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
				if (defaultDirectory == null) {
					obj = manager.resolveFile(defaultPath, path);
				} else {
					obj = manager.resolveFile(manager.resolveFile(defaultDirectory), path);
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
