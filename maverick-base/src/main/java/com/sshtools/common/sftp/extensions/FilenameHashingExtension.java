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
package com.sshtools.common.sftp.extensions;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger32;

public class FilenameHashingExtension extends FileHashingExtension {

	public static final String EXTENSION_NAME = "check-file-name";
    
	public FilenameHashingExtension() {
		super(EXTENSION_NAME);
	}
	
	protected byte[] getFileHandle(ByteArrayReader msg, SftpSubsystem sftp) throws IOException, PermissionDeniedException {
		String filename = msg.readString();
		AbstractFileSystem fs = sftp.getFileSystem();
		return fs.openFile(filename, new UnsignedInteger32(AbstractFileSystem.OPEN_READ), null);
	}
}
