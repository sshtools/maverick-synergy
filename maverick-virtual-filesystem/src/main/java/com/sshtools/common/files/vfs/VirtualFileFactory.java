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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.StringTokenizer;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.FileUtils;

public class VirtualFileFactory implements AbstractFileFactory<VirtualFile> {

	protected boolean cached = true;
	protected VirtualMountManager mgr;
	
	Map<String,VirtualFile> cache = null;
	
	public VirtualFileFactory(VirtualMountTemplate defaultMount,
			VirtualMountTemplate... additionalMounts) throws IOException, PermissionDeniedException {
		this.mgr = new VirtualMountManager(this, defaultMount, additionalMounts);
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

	public VirtualFile getFile(String path)
			throws PermissionDeniedException, IOException {

		String virtualPath;

		if (path.equals("")) {
			virtualPath = mgr.getDefaultMount().getMount();
			
		} else {
			virtualPath = canonicalisePath(path);
		}

		VirtualMount[] mounts = mgr.getMounts(virtualPath);
		if (!virtualPath.equals("") && mounts.length > 0) {
			String mountPath = FileUtils.addTrailingSlash(virtualPath);
			
			if (!mountPath.equals("/")) {
				for (VirtualMount m : mounts) {
					String thisMountPath = FileUtils.addTrailingSlash(m
							.getMount());
					if (thisMountPath.startsWith(mountPath) 
							&& !thisMountPath.contentEquals(mountPath)) {
						return new VirtualMountFile(
								FileUtils.removeTrailingSlash(virtualPath),
								mgr.getMount(virtualPath), this);
					}
				}
			} else {
				VirtualMount rootMount = mgr.getMount("/");
				if (!rootMount.isFilesystemRoot()
						|| (rootMount.isFilesystemRoot() && !rootMount
								.isDefault())) {
					return new VirtualMountFile(virtualPath, rootMount, this);
				}
			}
			// If we reached here we are file system root and default so we
			// don't use
			// the virtual mount file but instead list actual files and inject
			// any mounts
			// below us
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
