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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.sftp.extensions;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpSpecification;
import com.sshtools.common.util.ByteArrayReader;

public class PosixRenameExtension implements SftpExtension {

	public static final String POSIX_RENAME = "posix-rename@openssh.com";

	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSpecification sftp) {
		
		try {
			String oldpath = msg.readString();
			String newpath = msg.readString();
			
			sftp.getFileSystem().renameFile(oldpath, newpath);
		} catch (IOException e) {
			sftp.sendStatusMessage(requestId, SftpSpecification.STATUS_FX_FAILURE, e.getMessage());
		} catch (PermissionDeniedException e) {
			sftp.sendStatusMessage(requestId, SftpSpecification.STATUS_FX_PERMISSION_DENIED, e.getMessage());
		}
	}

	@Override
	public boolean supportsExtendedMessage(int messageId) {
		return false;
	}

	@Override
	public void processExtendedMessage(ByteArrayReader msg, SftpSpecification sftp) {
	}

}
