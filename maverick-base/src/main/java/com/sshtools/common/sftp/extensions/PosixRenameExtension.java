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
