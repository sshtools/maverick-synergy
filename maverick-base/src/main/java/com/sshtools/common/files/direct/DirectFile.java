
package com.sshtools.common.files.direct;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.util.UnsignedInteger64;

public class DirectFile extends AbstractDirectFile<DirectFile> {
	
	public DirectFile(String path, AbstractFileFactory<DirectFile> fileFactory, File homeDir) throws IOException {
		super(path, fileFactory, homeDir);
	}

	public SftpFileAttributes getAttributes() throws IOException {
		
		if(!f.exists()) {
			throw new FileNotFoundException();
		}
		
		SftpFileAttributes attrs = new SftpFileAttributes(getFileType(f), "UTF-8");
		
		attrs.setTimes(new UnsignedInteger64(f.lastModified() / 1000), 
				new UnsignedInteger64(f.lastModified() / 1000));
		
		attrs.setPermissions(String.format("%s%s-------", (isReadable() ? "r"
				: "-"), (isWritable() ? "w" : "-")));
		
		
		
		if(!isDirectory()) {
			attrs.setSize(new UnsignedInteger64(f.length()));
		}
		  
	    return attrs;
	}

	private int getFileType(File f) {
		if(f.isDirectory()) {
			return SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY;
		} else if(f.exists()) {
			return SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR;
		} else {
			return SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN;
		}
	}

	public List<AbstractFile> getChildren() throws IOException {
		
		File[] files = f.listFiles();
		if(files == null)
			throw new IOException(String.format("%s is unreadable.", f));
		List<AbstractFile> results = new ArrayList<AbstractFile>();
		for(File f : files) {
			results.add(new DirectFile(f.getAbsolutePath(), fileFactory, homeDir));
		}
		return results;
	}

	public AbstractFile resolveFile(String child) throws IOException,
			PermissionDeniedException {
		File file = new File(child);
		if(!file.isAbsolute()) {
			file = new File(f, child);
		}
		return new DirectFile(file.getAbsolutePath(), fileFactory, homeDir);
	}

	@Override
	public String readSymbolicLink() throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}
}
