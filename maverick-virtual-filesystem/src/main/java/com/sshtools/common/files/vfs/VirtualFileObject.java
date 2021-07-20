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

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileAdapter;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.FileUtils;

public class VirtualFileObject extends AbstractFileAdapter implements VirtualFile {

	
	Map<String,AbstractFile> mounts;
	protected VirtualFileFactory fileFactory;
	
	protected VirtualFileObject(VirtualFileFactory factory) {
		this.fileFactory = factory;
	}
	
	
	@Override
	public synchronized void refresh() {
		mounts = null;
		super.refresh();
	}


	protected synchronized Map<String,AbstractFile> getVirtualMounts() throws IOException, PermissionDeniedException {
		
		if(Objects.isNull(mounts)) {
			Map<String,AbstractFile> files = new HashMap<String,AbstractFile>();
			
			String currentPath = FileUtils.checkEndsWithSlash(getAbsolutePath());
	
			VirtualMountManager mgr = fileFactory.getMountManager();
			
			for(VirtualMount m : mgr.getMounts()) {
				String mpath = FileUtils.checkEndsWithSlash(m.getMount());
				if(mpath.startsWith(currentPath)) {
					if(!mpath.equals(currentPath)) {
						String child = m.getMount().substring(currentPath.length());
						if(child.indexOf('/') > -1) {
							child = child.substring(0,child.indexOf('/'));
						}
						files.put(currentPath + child, new VirtualMountFile(currentPath + child, m, fileFactory));
					}
				}
			}
			
			mounts =  files;
		}
		return mounts;
	}
}
