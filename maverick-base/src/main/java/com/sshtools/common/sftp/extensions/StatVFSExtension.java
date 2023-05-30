package com.sshtools.common.sftp.extensions;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger64;

public class StatVFSExtension extends AbstractSftpExtension {

	public static final String EXTENSION_NAME = "statvfs@openssh.com";

	public StatVFSExtension() {
		super(EXTENSION_NAME, true);
	}

	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSubsystem sftp) {
		try {
			var store = sftp.getFileSystem().getVolume(msg.readString());
			Packet reply = new Packet();
	        try {
	        	reply.write(SftpSubsystem.SSH_FXP_EXTENDED_REPLY);
	        	reply.writeInt(requestId);
	        	reply.writeUINT64(new UnsignedInteger64(store.blockSize()));
	        	reply.writeUINT64(new UnsignedInteger64(store.underlyingBlockSize()));
	        	reply.writeUINT64(new UnsignedInteger64(store.blocks()));
	        	reply.writeUINT64(new UnsignedInteger64(store.freeBlocks()));
	        	reply.writeUINT64(new UnsignedInteger64(store.userFreeBlocks()));
	        	reply.writeUINT64(new UnsignedInteger64(store.totalInodes()));
	        	reply.writeUINT64(new UnsignedInteger64(store.freeInodes()));
	        	reply.writeUINT64(new UnsignedInteger64(store.userFreeInodes()));
	        	reply.writeUINT64(new UnsignedInteger64(store.id()));
	        	reply.writeUINT64(new UnsignedInteger64(store.flags()));
	        	reply.writeUINT64(new UnsignedInteger64(store.maxFilenameLength()));
	        	sftp.sendMessage(reply);
	        } finally {
	        	reply.close();
	        }
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_OK, "The copy-file operation completed.");
		} catch (FileNotFoundException e) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_NO_SUCH_FILE, e.getMessage());
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