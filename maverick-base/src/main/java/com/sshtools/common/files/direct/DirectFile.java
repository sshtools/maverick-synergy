/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
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
		List<AbstractFile> results = new ArrayList<AbstractFile>();
		for(File f : files) {
			results.add(new DirectFile(f.getAbsolutePath(), fileFactory, homeDir));
		}
		return results;
	}

	public AbstractFile resolveFile(String child) throws IOException,
			PermissionDeniedException {
		return new DirectFile(new File(f, child).getAbsolutePath(), fileFactory, homeDir);
	}
}
