package com.sshtools.common.sftp.extensions;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.InvalidHandleException;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger64;

public class CopyDataSftpExtension extends AbstractSftpExtension {

	public static final String EXTENSION_NAME = "copy-data";

	public CopyDataSftpExtension() {
		super(EXTENSION_NAME, true);
	}

	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSubsystem sftp) {

		try {
			byte[] handle = msg.readBinaryString();
		    UnsignedInteger64 offset = msg.readUINT64();
		    UnsignedInteger64 length = msg.readUINT64();
		    
		    byte[] toHandle = msg.readBinaryString();
		    UnsignedInteger64 toOffset = msg.readUINT64();
		    
			AbstractFileSystem fs = sftp.getFileSystem();
			
			fs.copyData(handle, offset, length, toHandle, toOffset);
			
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_OK, "The copy-data operation completed.");
			
		} catch(IOException e) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_FAILURE, e.getMessage());
		} catch (PermissionDeniedException e) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_PERMISSION_DENIED, e.getMessage());
		} catch (InvalidHandleException e) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_NO_SUCH_FILE, e.getMessage());
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
