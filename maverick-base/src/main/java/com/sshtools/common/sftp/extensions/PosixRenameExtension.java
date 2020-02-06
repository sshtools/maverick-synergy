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
package com.sshtools.common.sftp.extensions;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;

public class PosixRenameExtension extends AbstractSftpExtension {

	public static final String EXTENSION_NAME = "posix-rename@openssh.com";

	public PosixRenameExtension() {
		super(EXTENSION_NAME, false);
	}
	
	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSubsystem sftp) {
		
		try {
			String oldpath = msg.readString();
			String newpath = msg.readString();
			
			AbstractFileSystem fs = sftp.getFileSystem();
			try {
				fs.getFileAttributes(newpath);
				fs.removeFile(newpath);
			} catch (FileNotFoundException e) {
			}
			
			sftp.getFileSystem().renameFile(oldpath, newpath);
			
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_OK, "Rename completed.");
		} catch (IOException e) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_FAILURE, e.getMessage());
		} catch (PermissionDeniedException e) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_PERMISSION_DENIED, e.getMessage());
		}
	}

	@Override
	public boolean supportsExtendedMessage(int messageId) {
		return false;
	}

	@Override
	public void processExtendedMessage(ByteArrayReader msg, SftpSubsystem sftp) {
	}

}
