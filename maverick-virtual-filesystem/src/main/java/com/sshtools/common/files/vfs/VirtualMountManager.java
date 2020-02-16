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

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.FileUtils;

public class VirtualMountManager {

	

	private SshConnection con;
	private VirtualMount defaultMount;
	private List<VirtualMount> mounts = new ArrayList<VirtualMount>();
	private VirtualFileFactory fileFactory;

	public VirtualMountManager(SshConnection con,
			VirtualFileFactory fileFactory) throws IOException,
			PermissionDeniedException {
		this(con, fileFactory, null);
	}

	public VirtualMountManager(SshConnection con,
			VirtualFileFactory fileFactory, VirtualMountTemplate homeMount,
			VirtualMountTemplate... additionalMounts) throws IOException,
			PermissionDeniedException {
		this.con = con;
		this.fileFactory = fileFactory;

		if (homeMount != null) {
			mounts.add(defaultMount = new VirtualMount(
					replaceTokens(homeMount.getMount(), con), replaceTokens(
							homeMount.getRoot(), con), fileFactory, homeMount
							.getActualFileFactory(), con, true, false));
		}

		// Add any remaining templates
		for (VirtualMountTemplate m : additionalMounts) {
			mounts.add(createMount(replaceTokens(m.getMount(), con), 
					replaceTokens(m.getRoot(), con),
					m.getActualFileFactory()));
		}

		sort();
	}

	private static String replaceTokens(String str,
			SshConnection con) {
		String ret = str.replace("${username}", con.getUsername());
		return ret;
	}
	
	public void mount(VirtualMountTemplate template) throws IOException, PermissionDeniedException {
		mount(createMount(template.getMount(),
				template.getRoot(), 
				template.getActualFileFactory()));
	}

	private void mount(VirtualMount mount) throws IOException,
			PermissionDeniedException {
		Log.info("Mounting " + mount.getMount() + " on " + mount.getRoot() + " for " + con.getUsername() + " (" + con.getSessionId() + ")");

		if(isMounted(mount.getMount())) {
			throw new IOException(mount.getMount() + " already mounted on " + getMount(mount.getMount()).getRoot());
		} 

		// Add the mount
		mounts.add(mount);
		sort();

		// Now test it
		try {
			AbstractFile f = fileFactory.getFile(mount.getMount(), con);
			f.createFolder();
		} catch (Exception ex) {
			Log.error(
					"Failed to mount " + mount.getMount() + " "
							+ mount.getRoot(), ex);
			unmount(mount);
		}

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
		Log.info("Unmounting " + mount.getMount() + " from " + mount.getRoot() + " for " + con.getUsername() + " (" + con.getSessionId() + ")");
		mounts.remove(mount);
		sort();
		Log.info("Unmounted " + mount.getMount() + " from " + mount.getRoot());
	}

	public VirtualMount getDefaultMount() {
		return defaultMount;
	}

	public VirtualMount[] getMounts() {
		return mounts.toArray(new VirtualMount[0]);
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
			AbstractFileFactory<?> actualFileFactory) throws IOException,
			PermissionDeniedException {
		return new VirtualMount(mount, path, fileFactory, actualFileFactory,
				con);
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

	public VirtualFileFactory getVirtualFileFactory() {
		return fileFactory;
	}

	public AbstractFileFactory<?> getDefaultFileFactory() {
		return fileFactory.getDefaultFileFactory();
	}

}