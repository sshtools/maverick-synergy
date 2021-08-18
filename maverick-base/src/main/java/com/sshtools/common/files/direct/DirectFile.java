/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
