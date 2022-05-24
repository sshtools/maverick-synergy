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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.StringTokenizer;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.FileUtils;

public class VirtualFileFactory implements AbstractFileFactory<VirtualFile> {

	protected boolean cached = true;
	protected VirtualMountManager mgr;
	
	Map<String,VirtualFile> cache = null;
	
	Map<String,VirtualMountFile> mountCache = new HashMap<>();
	
	public VirtualFileFactory(VirtualMountTemplate defaultMount,
			VirtualMountTemplate... additionalMounts) throws IOException, PermissionDeniedException {
		this.mgr = new VirtualMountManager(this, defaultMount, additionalMounts);
		
		for(VirtualMount mount : mgr.getMounts()) {
			String mountPath = FileUtils.addTrailingSlash(mount.getMount());
			mountCache.put(mountPath, new VirtualMountFile(mountPath, mount, this, false));
			for(String parentPath : FileUtils.getParentPaths(mountPath)) {
				/**
				 * Only write parent paths if an existing path does not exist (should allow 
				 * for root mounts to be defined by a parent but overwritten by a concrete mount.
				 */
				if(!mountCache.containsKey(parentPath)) {
					mountCache.put(parentPath, new VirtualMountFile(
							isRoot(parentPath) ? parentPath : FileUtils.checkEndsWithNoSlash(parentPath), 
								mgr.getMount(parentPath), this, true));
				}
			}
		}
	}

	private boolean isRoot(String path) {
		return path.equals("/");
	}
	
	public boolean isCached() {
		return cached;
	}

	public void setCached(boolean cached) {
		this.cached = cached;
	}


	private String canonicalisePath(String path) {
		StringTokenizer t = new StringTokenizer(path, "/", true);
		Stack<String> pathStack = new Stack<String>();
		while (t.hasMoreTokens()) {
			String e = t.nextToken();
			if (e.equals("..")) {
				if (pathStack.size() > 1) {
					pathStack.pop();
					pathStack.pop();
				}

			} else {
				if (pathStack.size() > 0 && pathStack.peek() == "/"
						&& e.equals("/")) {
					continue;
				}
				pathStack.push(e);
			}
		}
		String ret = "";
		for (String e : pathStack) {
			ret += e;
		}

		if (!ret.startsWith("/")) {
			ret = FileUtils
					.addTrailingSlash(mgr.getDefaultMount().getMount()) + ret;
		}
		return ret;

	}
	
	public Map<String,VirtualFile> resolveChildren(VirtualFile parent) throws PermissionDeniedException, IOException {
		
		Map<String,VirtualFile> files = new HashMap<>();
		
		AbstractFile file = parent.resolveFile();
		if(file.exists()) {
			for(AbstractFile child : file.getChildren()) {
				files.put(child.getName(), new VirtualMappedFile(child, parent.getMount(), this));
			}
		}

		String currentPath = FileUtils.checkEndsWithSlash(parent.getAbsolutePath());
		for(VirtualMount m : mgr.getMounts(currentPath)) {
			
			String mountPath = FileUtils.checkEndsWithSlash(m.getMount());
			
			if(mountPath.startsWith(currentPath) && !mountPath.equals(currentPath)) {
				String childPath = FileUtils.checkEndsWithNoSlash(mountPath.substring(currentPath.length()));
				List<String> childPaths = FileUtils.getParentPaths(childPath);
				boolean intermediate = false;
				if(intermediate = !childPaths.isEmpty()) {
					childPath = FileUtils.checkEndsWithNoSlash(childPaths.get(0));
				}
				files.put(childPath, new VirtualMountFile(currentPath + childPath, parent.getMount(), this, intermediate));
			}
		}
		
		return files;
		
	}

	public VirtualFile getFile(String path)
			throws PermissionDeniedException, IOException {

		String virtualPath;

		if (path.equals("")) {
			virtualPath = mgr.getDefaultMount().getMount();
			
		} else {
			virtualPath = canonicalisePath(path);
		}

		if(Log.isDebugEnabled()) {
			Log.debug("Resolved the following mounts for the path {}", path);
			for(VirtualMountFile m : mountCache.values()) {
				Log.debug("Mount {}", m.getAbsolutePath());
			}
		}
		
		if (!virtualPath.equals("") && mountCache.size() > 0) {
			String mountPath = FileUtils.addTrailingSlash(virtualPath);
			VirtualFile mountFile = mountCache.get(mountPath);
			if(Objects.nonNull(mountFile)) {
				return mountFile;
			}
		}

		if (!virtualPath.equals("/")) {
			virtualPath = FileUtils.removeTrailingSlash(virtualPath);
		}

		VirtualMount m = mgr.getMount(virtualPath);
		VirtualFile cached = getCachedObject(virtualPath);
		if(Objects.nonNull(cached)) {
			return cached;
		}
		VirtualFile f = new VirtualMappedFile(virtualPath, m, this);
		if (m.isCached()) {
			cacheObject(f);
		}
		return f;

	}

	private void cacheObject(VirtualFile f) throws IOException, PermissionDeniedException {
		if(Objects.isNull(cache)) {
			cache = new HashMap<>();
		}
		
		cache.put(f.getAbsolutePath(), f);
	}

	protected VirtualFile getCachedObject(String virtualPath) {
		if(Objects.nonNull(cache)) {
			cache.get(virtualPath);
		}
		return null;
	}

	public VirtualMountManager getMountManager()
			throws IOException, PermissionDeniedException {
		return mgr;
	}

	public Event populateEvent(Event evt) {
		try {
			return evt
					.addAttribute(
							EventCodes.ATTRIBUTE_MOUNT_MANAGER,
							getMountManager());
		} catch (Exception e) {
			return evt;
		}
	}

	public VirtualFile getDefaultPath()
			throws PermissionDeniedException, IOException {
		return getFile("");
	}
}
