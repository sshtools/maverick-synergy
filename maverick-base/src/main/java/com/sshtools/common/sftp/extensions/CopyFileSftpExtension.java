package com.sshtools.common.sftp.extensions;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;

public class CopyFileSftpExtension extends AbstractSftpExtension {

	public static final String EXTENSION_NAME = "copy-file";

	protected CopyFileSftpExtension() {
		super(EXTENSION_NAME, true);
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
