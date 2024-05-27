package com.sshtools.common.files.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileAdapter;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder;

public abstract class VirtualFileObject extends AbstractFileAdapter implements VirtualFile {

	VirtualMount parentMount;
	Map<String,AbstractFile> mounts;
	protected VirtualFileFactory fileFactory;
	
	protected VirtualFileObject(VirtualFileFactory factory, VirtualMount parentMount) {
		this.fileFactory = factory;
		this.parentMount = parentMount;
	}
	
	
	@Override
	public synchronized void refresh() {
		mounts = null;
		super.refresh();
	}

	public VirtualMount getMount() {
		return parentMount;
	}

	@Override
	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return fileFactory;
	}


	@Override
	public SftpFileAttributes getAttributes() throws IOException, PermissionDeniedException {
		
		if(!exists()) {
			throw new FileNotFoundException();
		}
		
		var bldr = SftpFileAttributesBuilder.ofType(
				isDirectory() ? SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY : SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR,
						"UTF-8");
		bldr.withSize(length());
		
		var permBldr = PosixPermissionsBuilder.create();
		
		if(isReadable()) {
			permBldr.withAllRead();
		}
		if(isWritable()) {
			permBldr.withAllWrite();
		}
		if(isDirectory()) {
			permBldr.withAllExecute();
		}

		
		bldr.withPermissions(permBldr.build());
		
		bldr.withUid(0);
		bldr.withGid(0);
		bldr.withUsername(System.getProperty("maverick.unknownUsername", "unknown"));
		bldr.withGroup(System.getProperty("maverick.unknownUsername", "unknown"));
		bldr.withLastModifiedTime(lastModified());
		bldr.withLastAccessTime(lastModified());

		return bldr.build();
	}
	
	

}
