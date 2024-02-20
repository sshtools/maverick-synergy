package com.sshtools.common.sftp.extensions;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.sftp.UnsupportedFileOperationException;
import com.sshtools.common.util.ByteArrayReader;

public class HardLinkExtension extends AbstractSftpExtension {

	public static final String EXTENSION_NAME = "hardlink@openssh.com";

	public HardLinkExtension() {
		super(EXTENSION_NAME, true);
	}

	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSubsystem sftp) {
		try {
			sftp.getFileSystem().createLink(msg.readString(), msg.readString());
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_OK, "The hard link operation completed.");

		} catch (IOException e) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_FAILURE, e.getMessage());
		} catch (PermissionDeniedException e) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_PERMISSION_DENIED, e.getMessage());
		} catch (UnsupportedFileOperationException e) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_OP_UNSUPPORTED, e.getMessage());
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
