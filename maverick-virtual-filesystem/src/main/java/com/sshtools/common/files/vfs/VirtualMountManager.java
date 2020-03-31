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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.FileUtils;

public class VirtualMountManager {

	private VirtualMount defaultMount;
	private List<VirtualMount> mounts = new ArrayList<VirtualMount>();
	private VirtualFileFactory fileFactory;
	private ThreadLocal<VirtualMount> testingMount = new ThreadLocal<>(); 
	
	public VirtualMountManager(VirtualFileFactory fileFactory) throws IOException,
			PermissionDeniedException {
		this(fileFactory, null);
	}

	public VirtualMountManager(VirtualFileFactory fileFactory, VirtualMountTemplate homeMount,
			VirtualMountTemplate... additionalMounts) throws IOException,
			PermissionDeniedException {

		this.fileFactory = fileFactory;

		if (homeMount != null) {
			defaultMount = new VirtualMount(homeMount.getMount(), 
					homeMount.getRoot(), fileFactory, homeMount.getActualFileFactory(), true, false, homeMount.isCreateMountFolder());
			if(defaultMount.isCreateMountFolder()) {
				defaultMount.getActualFileFactory().getFile(defaultMount.getRoot()).createFolder();
			}
			mounts.add(defaultMount);
		}

		// Add any remaining templates
		for (VirtualMountTemplate m : additionalMounts) {
			VirtualMount vm = createMount(m.getMount(), 
					m.getRoot(),
					m.getActualFileFactory(),
					m.isCreateMountFolder());
			

			if(vm.isCreateMountFolder()) {
				vm.getActualFileFactory().getFile(vm.getRoot()).createFolder();
			}
			mounts.add(vm);
		}

		sort();
	}
	
	public void mount(VirtualMountTemplate template) throws IOException, PermissionDeniedException {
		mount(template, false);
	}
	
	public void mount(VirtualMountTemplate template, boolean unmount) throws IOException, PermissionDeniedException {
		mount(createMount(template.getMount(),
				template.getRoot(), 
				template.getActualFileFactory(),
				template.isCreateMountFolder()), unmount);
	}

	public void test(VirtualMountTemplate template) throws IOException, PermissionDeniedException {
		
		test(createMount(template.getMount(),
				template.getRoot(), 
				template.getActualFileFactory(),
				template.isCreateMountFolder()));
		
	}
	
	private void test(VirtualMount mount) throws IOException, PermissionDeniedException {
		
		Log.info("Testing " + mount.getMount() + " on " + mount.getRoot());

		try {
			testingMount.set(mount);
			AbstractFile f = fileFactory.getFile(mount.getMount());
			if(mount.isCreateMountFolder()) {
				f.createFolder();
			}
		} catch (Exception ex) {
			Log.error("Cannot mount " + mount.getMount() + " " + mount.getRoot(), ex);
		} finally {
			testingMount.remove();
		}
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

		test(mount);
		
		// Add the mount
		mounts.add(mount);
		sort();

		Log.info("Mounted " + mount.getMount() + " on " + mount.getRoot());

	}

	private void sort() {
		Collections.sort(mounts, new Comparator<AbstractMount>() {

			public int compare(AbstractMount o1, AbstractMount o2) {
				return o1.getMount().compareTo(o2.getMount()) * -1;
			}

		});
	}

	public void unmount(VirtualMount mount) throws IOException {
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
		sort();
		Log.info("Unmounted " + mounted.getMount() + " from " + mounted.getRoot());
	}

	public VirtualMount getDefaultMount() {
		return defaultMount;
	}

	public VirtualMount[] getMounts() {
		List<VirtualMount> tmp = new ArrayList<>();
		VirtualMount testMount = testingMount.get();
		if(testMount!=null) {
			tmp.add(testingMount.get());
		}
		tmp.addAll(mounts);
		return tmp.toArray(new VirtualMount[0]);
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

	private VirtualMount createMount(String mount, String path,
			AbstractFileFactory<?> actualFileFactory, boolean createMoundFolder) throws IOException,
			PermissionDeniedException {
		return new VirtualMount(mount, path, fileFactory, actualFileFactory, createMoundFolder);
	}

	public VirtualMount getMount(String path) throws IOException {

		path = path.replace('\\', '/').trim();

		if (path.equals("") || path.equals(".") || path.startsWith("./")) {
			return defaultMount;
		}

		for (VirtualMount mount : getMounts()) {
			String mountPath = FileUtils.checkEndsWithSlash(mount.getMount());
			path = FileUtils.checkEndsWithSlash(path);
			if (path.startsWith(mountPath)) {
				return mount;
			}
		}
		throw new FileNotFoundException("No mount for " + path);
	}

	public VirtualMount[] getMounts(String path) {
		if (path.equals("")) {
			return new VirtualMount[] { defaultMount };
		}

		path = FileUtils.addTrailingSlash(path);

		List<VirtualMount> matched = new ArrayList<VirtualMount>();
		for (VirtualMount m : getMounts()) {
			String mountPath = FileUtils.addTrailingSlash(m.getMount());
			if (path.startsWith(mountPath) || mountPath.startsWith(path)) {
				matched.add(m);
			}
		}
		return matched.toArray(new VirtualMount[0]);
	}

	public VirtualFileFactory getVirtualFileFactory() {
		return fileFactory;
	}

}