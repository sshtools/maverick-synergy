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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileAdapter;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder;

public abstract class VirtualFileObject extends AbstractFileAdapter implements VirtualFile {

	VirtualMount parentMount;
	Map<String,AbstractFile> mounts;
	protected VirtualFileFactory fileFactory;
	
	protected VirtualFileObject(VirtualFileFactory factory, VirtualMount parentMount) {
		this.fileFactory = factory;
		this.parentMount = parentMount;
	}
	
	
	@Override
	public synchronized void refresh() {
		mounts = null;
		super.refresh();
	}

	public VirtualMount getMount() {
		return parentMount;
	}
	
	@Deprecated
	/**
	 * @deprecated Use getMount instead as it's now part of the VirtualFile interface.
	 * @return
	 */
	public VirtualMount getParentMount() {
		return parentMount;
	}

	@Override
	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return fileFactory;
	}


	@Override
	public SftpFileAttributes getAttributes() throws IOException, PermissionDeniedException {
		
		if(!exists()) {
			throw new FileNotFoundException();
		}
		
		var bldr = SftpFileAttributesBuilder.ofType(
				isDirectory() ? SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY : SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR,
						"UTF-8");
		bldr.withSize(length());
		
		var permBldr = PosixPermissionsBuilder.create();
		
		if(isReadable()) {
			permBldr.withAllRead();
		}
		if(isWritable()) {
			permBldr.withAllWrite();
		}
		if(isDirectory()) {
			permBldr.withAllExecute();
		}

		
		bldr.withPermissions(permBldr.build());
		
		bldr.withUid(0);
		bldr.withGid(0);
		bldr.withUsername(System.getProperty("maverick.unknownUsername", "unknown"));
		bldr.withGroup(System.getProperty("maverick.unknownUsername", "unknown"));
		bldr.withLastModifiedTime(lastModified());
		bldr.withLastAccessTime(lastModified());

		return bldr.build();
	}
	
	

}
