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

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;

public class CopyFileSftpExtension extends AbstractSftpExtension {

	public static final String EXTENSION_NAME = "copy-file";

	protected CopyFileSftpExtension() {
		super(EXTENSION_NAME, false);
	}

	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSubsystem sftp) {

		try {
			String source = msg.readString();
			String destination = msg.readString();
			boolean overwrite = msg.readBoolean();
			
			AbstractFileSystem fs = sftp.getFileSystem();
			
			fs.copyFile(source, destination, overwrite);
			
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_OK, "The copy-file operation completed.");
			
		} catch(IOException e) {
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
