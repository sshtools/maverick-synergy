package com.sshtools.common.sftp.extensions;

import java.io.IOException;
import java.util.Optional;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger32;

public class FilenameHashingExtension extends FileHashingExtension {

	public static final String EXTENSION_NAME = "check-file-name";
    
	public FilenameHashingExtension() {
		super(EXTENSION_NAME);
	}
	
	protected byte[] getFileHandle(ByteArrayReader msg, SftpSubsystem sftp) throws IOException, PermissionDeniedException {
		String filename = msg.readString();
		AbstractFileSystem fs = sftp.getFileSystem();
		return fs.openFile(filename, new UnsignedInteger32(AbstractFileSystem.OPEN_READ), Optional.empty(), null);
	}
}
