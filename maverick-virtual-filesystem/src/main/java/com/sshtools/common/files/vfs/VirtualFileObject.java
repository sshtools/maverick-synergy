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
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.util.UnsignedInteger64;

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
		
		SftpFileAttributes attrs = new SftpFileAttributes(
				isDirectory() ? SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY : SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR,
						"UTF-8");
		attrs.setSize(new UnsignedInteger64(length()));
		UnsignedInteger64 t = new UnsignedInteger64(lastModified());
		
		PosixPermissionsBuilder builder = PosixPermissionsBuilder.create();
		
		if(isReadable()) {
			builder.withAllRead();
		}
		if(isWritable()) {
			builder.withAllWrite();
		}
		if(isDirectory()) {
			builder.withAllExecute();
		}

		
		attrs.setPermissions(builder.build());
		
		attrs.setUID("0");
		attrs.setGID("0");
		attrs.setUsername(System.getProperty("maverick.unknownUsername", "unknown"));
		attrs.setGroup(System.getProperty("maverick.unknownUsername", "unknown"));
		
		attrs.setTimes(t, t);

		return attrs;
	}
	
	

}
