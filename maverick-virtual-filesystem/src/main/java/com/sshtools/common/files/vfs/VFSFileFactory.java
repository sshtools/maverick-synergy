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
import com.sshtools.common.permissions.PermissionDeniedException;

public class VFSFileFactory implements AbstractFileFactory<VFSFile> {
	
	private FileSystemOptions opts;
//	private FileObject defaultPath;
	private boolean useURI = true;
	private FileSystemManager manager;
//	private String homeDirectory;

	public VFSFileFactory() throws FileNotFoundException {
		this((FileSystemManager) null);
	}
	
	@Deprecated
	public VFSFileFactory(String homeDirectory) throws FileNotFoundException {
		this((FileSystemManager) null, homeDirectory);
	}

	public VFSFileFactory(FileSystemOptions opts) throws FileNotFoundException {
		this((FileSystemManager) null, opts, null);
	}

	@Deprecated
	public VFSFileFactory(FileSystemOptions opts, String homeDirectory) throws FileNotFoundException {
		this((FileSystemManager) null, opts, homeDirectory);
	}

	public VFSFileFactory(FileSystemManager manager) throws FileNotFoundException {
		this(manager, new FileSystemOptions(), null);
	}

	@Deprecated
	public VFSFileFactory(FileSystemManager manager, String homeDirectory) throws FileNotFoundException {
		this(manager, new FileSystemOptions(), homeDirectory);
	}

	public VFSFileFactory(FileSystemManager manager, FileSystemOptions opts) throws FileNotFoundException {
		this(manager, opts, null);
	}

	@Deprecated
	public VFSFileFactory(FileSystemManager manager, FileSystemOptions opts, String homeDirectory)
			throws FileNotFoundException {
		this.opts = opts;

		try {
			if (manager == null) {
				manager = VFS.getManager();
			}
		} catch (FileSystemException fse) {
			throw new FileNotFoundException("Could not obtain VFS manager.");
		}
		this.manager = manager;
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
			throw new FileNotFoundException("Path " + path + " was not found");
		}
	}

	public VFSFile getFile(String path) throws PermissionDeniedException, IOException {
		FileObject obj;
		try {
			obj = manager.resolveFile(path, opts);
		} catch (FileSystemException e) {
			throw new FileNotFoundException("Path " + path + " was not found");
		}
		return new VFSFile(obj, this);
	}

	public Event populateEvent(Event evt) {
		return evt;
	}

	@Deprecated
	public void setDefaultPath(String path) throws FileNotFoundException {

	}

	public VFSFile getDefaultPath() throws PermissionDeniedException, IOException {
		return getFile("");
	}
}
