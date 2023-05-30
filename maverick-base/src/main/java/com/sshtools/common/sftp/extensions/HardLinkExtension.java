package com.sshtools.common.sftp.extensions;

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