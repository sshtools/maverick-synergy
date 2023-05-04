package com.sshtools.common.sftp.extensions;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;

public class FileHandleHashingExtension extends FileHashingExtension {

	public static final String EXTENSION_NAME = "check-file-handle";
    
	public FileHandleHashingExtension() {
		super(EXTENSION_NAME);
	}

	protected byte[] getFileHandle(ByteArrayReader msg, SftpSubsystem sftp) throws IOException, PermissionDeniedException {
		return msg.readBinaryString();
	}
}
