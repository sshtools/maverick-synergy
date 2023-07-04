/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.StringTokenizer;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.FileUtils;

public class VirtualFileFactory implements AbstractFileFactory<VirtualFile> {

	protected boolean cached = true;
	
	Map<String,VirtualFile> cache = new HashMap<>();
	List<VirtualMount> mounts = new ArrayList<VirtualMount>();
	Map<String,VirtualMountFile> mountCache = new HashMap<>();
	private VirtualMount defaultMount;
	
	public VirtualFileFactory(VirtualMountTemplate defaultMount,
			VirtualMountTemplate... additionalMounts) throws IOException, PermissionDeniedException {
		setupMounts(defaultMount, additionalMounts);
	}

	public VirtualMount getMount(String path) throws IOException {

		path = path.replace('\\', '/').trim();

		if (path.equals("") || path.equals(".") || path.startsWith("./")) {
			return defaultMount;
		}

		for (VirtualMount mount : mounts) {
			String mountPath = FileUtils.checkEndsWithSlash(mount.getMount());
			path = FileUtils.checkEndsWithSlash(path);
			if (path.startsWith(mountPath)) {
				return mount;
			}
		}
		throw new FileNotFoundException("No mount for " + path);
	}
	
	private void setupMounts(VirtualMountTemplate homeMount,
			VirtualMountTemplate... additionalMounts) throws IOException, PermissionDeniedException {
		
		if (homeMount != null) {
			defaultMount = new VirtualMount(homeMount, 
					this, homeMount.getActualFileFactory(), 
					true, false, homeMount.isCreateMountFolder(),
					homeMount.lastModified());
			if(defaultMount.isCreateMountFolder()) {
				defaultMount.getActualFileFactory().getFile(defaultMount.getRoot()).createFolder();
			}
			mounts.add(defaultMount);
		}

		// Add any remaining templates
		for (VirtualMountTemplate m : additionalMounts) {
			VirtualMount vm = createMount(m,
					m.getActualFileFactory(),
					m.isCreateMountFolder(),
					m.lastModified());
			

			if(vm.isCreateMountFolder()) {
				vm.getActualFileFactory().getFile(vm.getRoot()).createFolder();
			}
			mounts.add(vm);
		}

		sort(mounts);
		
		rebuildMountCache();
	}
	
	private void rebuildMountCache() throws PermissionDeniedException, IOException {
		
		mountCache.clear();
		cache.clear();
		
		for(VirtualMount mount : mounts) {
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
								getMount(parentPath), this, true));
				}
			}
		}
	}
	private void sort(List<VirtualMount> mounts) {
		Collections.sort(mounts, new Comparator<VirtualMount>() {

			@Override
			public int compare(VirtualMount o1, VirtualMount o2) {
				if(o1.isParentOf(o2)) {
					return 1;
				} else if(o1.isChildOf(o2)) {
					return -1;
				} else {
					return o2.getMount().compareTo(o1.getMount());
				}
			}
			
		});
		
		if(Log.isDebugEnabled()) {
			Log.debug("Sorting mounts by path and with child relationship preferred");
			for(VirtualMount m : mounts) {
				Log.debug("Mount {} on {}", m.getMount(), m.getRoot());
			}
		}
	}
	
	private VirtualMount createMount(VirtualMountTemplate template,
			AbstractFileFactory<?> actualFileFactory, boolean createMoundFolder, long lastModified) throws IOException,
			PermissionDeniedException {
		return new VirtualMount(template, this, actualFileFactory, createMoundFolder, lastModified);
	}
	
	public VirtualMount[] getMounts(String path) {
		if (path.equals("")) {
			return new VirtualMount[] { defaultMount };
		}

		path = FileUtils.addTrailingSlash(path);

		List<VirtualMount> matched = new ArrayList<VirtualMount>();
		for (VirtualMount m : mounts) {
			String mountPath = FileUtils.addTrailingSlash(m.getMount());
			if (path.startsWith(mountPath) || mountPath.startsWith(path)) {
				matched.add(m);
			}
		}		
		
		return matched.toArray(new VirtualMount[0]);
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
					.addTrailingSlash(defaultMount.getMount()) + ret;
		}
		return ret;

	}
	
	public Map<String,VirtualFile> resolveChildren(VirtualFile parent) throws PermissionDeniedException, IOException {
		
		Map<String,VirtualFile> files = new HashMap<>();
		
		AbstractFile file = parent.resolveFile();

		/**
		 * This check is important because in some circumstances the mount might be a path
		 * within a mount that has no hard backing. For example when / and /public/foo are
		 * mounted, /public is not a real path and we don't want to generate errors because
		 * we have to list foo as a directory under /public.
		 */
		if(file.isDirectory()) {
			for(AbstractFile child : file.getChildren()) {
				files.put(child.getName(), new VirtualMappedFile(child, parent.getMount(), this));
			}
		}
		
		String currentPath = FileUtils.checkEndsWithSlash(parent.getAbsolutePath());
		for(VirtualMount m : getMounts(currentPath)) {
			
			String mountPath = FileUtils.checkEndsWithSlash(m.getMount());
			
			if(mountPath.startsWith(currentPath) && !mountPath.equals(currentPath)) {
				String childPath = FileUtils.checkEndsWithNoSlash(mountPath.substring(currentPath.length()));
				List<String> childPaths = FileUtils.getParentPaths(childPath);
				Collections.reverse(childPaths);
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
			virtualPath = defaultMount.getMount();
			
		} else {
			virtualPath = canonicalisePath(path);
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

		VirtualMount m = getMount(virtualPath);
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

	public VirtualFile getDefaultPath()
			throws PermissionDeniedException, IOException {
		return getFile("");
	}
	
	public void mount(VirtualMountTemplate template) throws IOException, PermissionDeniedException {
		mount(template, false);
	}
	
	public void mount(VirtualMountTemplate template, boolean unmount) throws IOException, PermissionDeniedException {
		mount(createMount(template, 
				template.getActualFileFactory(),
				template.isCreateMountFolder(),
				template.lastModified()), unmount);
	}
	
	private void mount(VirtualMount mount, boolean unmount) throws IOException,
			PermissionDeniedException {
		
		if(unmount && isMounted(mount.getMount())) {
			unmount(mount);
		}

		if(isMounted(mount.getMount())) {
			throw new IOException(mount.getMount() + " already mounted on " + getMount(mount.getMount()).getRoot());
		} 
		
		Log.info("Mounting " + mount.getMount() + " on " + mount.getRoot());
		
		// Add the mount
		mounts.add(mount);
		sort(mounts);
		rebuildMountCache();
		Log.info("Mounted " + mount.getMount() + " on " + mount.getRoot());

	}

	public void unmount(VirtualMount mount) throws IOException, PermissionDeniedException {
		Log.info("Unmounting " + mount.getMount() + " from " + mount.getRoot());
		VirtualMount mounted = null;
		for(VirtualMount m : mounts) {
			if(FileUtils.checkEndsWithSlash(m.getMount())
					.equals(FileUtils.checkEndsWithSlash(mount.getMount()))) {
				mounted = m;
			}
		}
		if(Objects.isNull(mounted)) {
			throw new IOException(String.format("Could not find mount %s", mount.getMount()));
		}
		mounts.remove(mounted);
		sort(mounts);
		rebuildMountCache();
	}

	public VirtualMount getDefaultMount() {
		return defaultMount;
	}

	public boolean isMounted(String path) {
		if (path.equals("")) {
			return true;
		}

		for (VirtualMount mount : mounts) {
			if (FileUtils.addTrailingSlash(mount.getMount()).equals(
					FileUtils.addTrailingSlash(path))) {
				return true;
			}
		}
		return false;
	}

}
