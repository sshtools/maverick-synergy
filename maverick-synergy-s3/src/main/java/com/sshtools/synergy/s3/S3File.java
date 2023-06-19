package com.sshtools.synergy.s3;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder;

public interface S3File extends AbstractFile {

	default public SftpFileAttributes getAttributes() throws FileNotFoundException, IOException, PermissionDeniedException {
		
		if(!exists()) {
			throw new FileNotFoundException();
		}
		
		var bldr = SftpFileAttributesBuilder.ofType(
				isDirectory() ? SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY : SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR,
						"UTF-8");
		bldr.withSize(length());
		
		var permBuilder = PosixPermissionsBuilder.create();
		if(isDirectory()) {
			permBuilder.withAllExecute();
		}
		if(isReadable()) {
			permBuilder.withAllRead();
		}
		if(isWritable()) {
			permBuilder.withAllWrite();
		}
		
		bldr.withPermissions(permBuilder.build());
		
		bldr.withUid(0);
		bldr.withGid(0);
		bldr.withUsername(System.getProperty("maverick.unknownUsername", "unknown"));
		bldr.withGroup(System.getProperty("maverick.unknownUsername", "unknown"));
		bldr.withLastModifiedTime(lastModified());
		bldr.withLastAccessTime(lastModified());
		return bldr.build();
	}
}
