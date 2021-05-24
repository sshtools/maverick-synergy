/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.util.UnsignedInteger64;

public class DirectFileJava7 extends DirectFile {
	
	public DirectFileJava7(String path, AbstractFileFactory<DirectFile> fileFactory, File homeDir) throws IOException {
		super(path, fileFactory, homeDir);
	}

	@Override
	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		Path file = FileSystems.getDefault().getPath(f.getAbsolutePath());
		if(attrs.hasModifiedTime()) {
			Files.setLastModifiedTime(file, FileTime.fromMillis(attrs.getModifiedTime().longValue() * 1000));
		}
		String str = attrs.getPermissionsString();
		if(str != null && str.length() == 10) {
			String current = null;
			try {
				current = PosixFilePermissions.toString(Files.getPosixFilePermissions(file));
			}
			catch(UnsupportedOperationException uoe) {
			}
			if(current != null) {
				String wanted = str.substring(1);
				if(!Objects.equals(current, wanted))
					Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(wanted));
			}
		}
		if(attrs.hasUID()) {
		}
	}

	@Override
	public SftpFileAttributes getAttributes() throws IOException {
		
		if(!f.exists())
			throw new FileNotFoundException();
		
		
		Path file = FileSystems.getDefault().getPath(f.getAbsolutePath());
		
		BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
		SftpFileAttributes attrs = new SftpFileAttributes(getFileType(attr), "UTF-8");
		
		try {
		
			attrs.setTimes(new UnsignedInteger64(attr.lastAccessTime().toMillis() / 1000), 
					new UnsignedInteger64(attr.lastModifiedTime().toMillis() / 1000));
			
			attrs.setSize(new UnsignedInteger64(attr.size()));

			try {
				PosixFileAttributes posix =  Files.readAttributes(file, PosixFileAttributes.class);
				
				attrs.setGID(posix.group().getName());
				attrs.setUID(posix.owner().getName());
				
				attrs.setPermissions(PosixFilePermissions.toString(posix.permissions()));
				
				hidden = f.getName().startsWith(".");

				// We return now as we have enough information
				return attrs;
				
			} catch (UnsupportedOperationException | IOException e) {
			}

			
			try {
				DosFileAttributes dos = Files.readAttributes(file,
							DosFileAttributes.class);
			
				hidden = dos.isHidden();
			
				String read = "r";
				String write = dos.isReadOnly() ? "-" : "w";
				String exe = (f.getName().endsWith(".exe") 
						|| f.getName().endsWith(".com") 
						|| f.getName().endsWith(".cmd")) ? "x" : "-";
				
				attrs.setPermissions(read + write + exe + read + write + exe + read + write + exe);
		
			} catch(UnsupportedOperationException | IOException e) {
			}
			
			
		} catch (UnsupportedOperationException e) {
		}
		
		return attrs;
		  
	    
	}

	private int getFileType(BasicFileAttributes attr) {
		if(attr.isDirectory())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY;
		if(attr.isRegularFile())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR;
		if(attr.isSymbolicLink())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_SYMLINK;
		if(attr.isOther())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_SPECIAL;
		
		return SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN;
	}

	@Override
	public List<AbstractFile> getChildren() throws IOException {
		
		File[] files = f.listFiles();
		if(files == null)
			throw new IOException(String.format("%s is unreadable.", f));
		List<AbstractFile> files2 = new ArrayList<AbstractFile>();
		for(File f : files) {
			files2.add(new DirectFileJava7(f.getAbsolutePath(), fileFactory, homeDir));
		}
		return files2;
	}

	@Override
	public AbstractFile resolveFile(String child) throws IOException,
			PermissionDeniedException {
		File file = new File(child);
		if(!file.isAbsolute()) {
			file = new File(f, child);
		}
		return new DirectFileJava7(file.getAbsolutePath(), fileFactory, homeDir);
	}

	@Override
	public void linkTo(String target) throws IOException, PermissionDeniedException {
		Files.createLink(f.toPath(), target.startsWith("/") ? fileFactory.getFile(target).f.toPath() : homeDir.toPath().resolve(target));
	}

	@Override
	public void symlinkTo(String target) throws IOException, PermissionDeniedException {
		Files.createSymbolicLink(f.toPath(), target.startsWith("/") ? fileFactory.getFile(target).f.toPath() : homeDir.toPath().resolve(target));
	}

	@Override
	public String readSymbolicLink() throws IOException, PermissionDeniedException {
		return Files.readSymbolicLink(f.toPath()).toString();
	}
}
