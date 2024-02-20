package com.sshtools.common.sftp;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.FileVolume;
import com.sshtools.common.files.direct.NioFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

/**
 * This class is final. It may not be extended.
 * @author Lee David Painter
 */
public final class AbstractFileSystem {

	/** File open flag, opens the file for reading. */
    public static final int OPEN_READ = 0x00000001;

    /** File open flag, opens the file for writing. */
    public static final int OPEN_WRITE = 0x00000002;

    /** File open flag, forces all writes to append data at the end of the file. */
    public static final int OPEN_APPEND = 0x00000004;

    /**
     * File open flag, if specified a new file will be created if one does not
     * already exist.
     */
    public static final int OPEN_CREATE = 0x00000008;

    /**
     * File open flag, forces an existing file with the same name to be
     * truncated to zero length when creating a file by specifying OPEN_CREATE.
     */
    public static final int OPEN_TRUNCATE = 0x00000010;

    /**
     * File open flag, causes an open request to fail if the named file already
     * exists. OPEN_CREATE must also be specified if this flag is used.
     */
    public static final int OPEN_EXCLUSIVE = 0x00000020;

    /**
     * Indicates the server should treat the file as text and convert
     * it to the canoncial newline convention in use.
     */
    public static final int OPEN_TEXT = 0x00000040;
    
	public static final int BLOCK_READ 			= 0x00000040;
	public static final int BLOCK_WRITE 			= 0x00000080;
	public static final int BLOCK_DELETE			= 0x00000100;
	public static final int BLOCK_ADVISORY		= 0x00000200;
    
    public static final String AUTHORIZED_KEYS_STORE = "authorized_keys";
    public static final String SFTP = "sftp";
    public static final String SCP = "scp";
	public static final String SHELL = "shell";
	
	protected Map<String, OpenFile> openFiles = new ConcurrentHashMap<String, OpenFile>(8, 0.9f, 1);
	protected Map<String, OpenDirectory> openDirectories = new ConcurrentHashMap<String, OpenDirectory>(8, 0.9f, 1);
	protected AbstractFileFactory<?> fileFactory;

	static Set<String> defaultPaths = new HashSet<String>(Arrays.asList("", ".", "./"));

	final SshConnection con;
	final String protocolInUse;

	Map<String,MultipartTransfer> multipartUploads = new HashMap<>();
	
	public AbstractFileSystem(SshConnection con, String protocolInUse) throws IOException, PermissionDeniedException {
		this.fileFactory = con.getContext().getPolicy(FileSystemPolicy.class).getFileFactory().getFileFactory(con);
		this.con = con;
		this.protocolInUse = protocolInUse;

		if(Log.isDebugEnabled())
			Log.debug("Completed Abstract File System Initialization");

	}

	public AbstractFileFactory<?> getFileFactory() {
		return fileFactory;
	}

	public void init(SshConnection con, String protocolInUse) {
		// Deprecated
	}

	protected AbstractFile resolveFile(String path, SshConnection con) throws PermissionDeniedException, IOException {
		if(Objects.isNull(fileFactory)) {
			throw new PermissionDeniedException("The user does not have access to a file system.");
		}
		if (defaultPaths.contains(path)) {
			return fileFactory.getDefaultPath();
		} else {
			return fileFactory.getFile(path);
		}
	}

	public void closeFilesystem() {
		String obj;
		for (Iterator<String> it = openFiles.keySet().iterator(); it.hasNext();) {
			obj = it.next();
			try {
				closeFile(obj);
			} catch (Exception ex) {
				if(Log.isErrorEnabled()) {
					Log.error("Error closing file", ex);
				}
			}
			it.remove();
		}
		for (Iterator<String> it = openDirectories.keySet().iterator(); it.hasNext();) {
			obj = it.next();
			try {
				closeFile(obj);
			} catch (Exception ex) {
				if(Log.isErrorEnabled()) {
					Log.error("Error closing directory", ex);
				}
			}
			it.remove();
		}

	}

	public boolean makeDirectory(String path, SftpFileAttributes attrs)
			throws PermissionDeniedException, FileNotFoundException, IOException {

		if(Log.isDebugEnabled())
			Log.debug("Creating directory " + path);

		if(path.equals("/")) {
			throw new PermissionDeniedException("Unable to create root file");
		}
		String parentPath = FileUtils.getParentPath(path);
		AbstractFile parent = resolveFile(parentPath, con);
		
		if (!parent.isWritable()) {
			throw new PermissionDeniedException("The user does not have permission to write/create in " + parentPath);
		}
		
		if(getConnection().getContext().getPolicy(FileSystemPolicy.class).isMkdirParentMustExist() && !parent.exists()) {
			throw new FileNotFoundException("The parent folder does not exist!");
		}
		
		AbstractFile f = resolveFile(path, con);
		
		if (f.createFolder()) {
			f.setAttributes(attrs);

			boolean exists = f.exists();
			return exists;
		} else {
			return false;
		}
	}

	public SftpFileAttributes getFileAttributes(byte[] handle)
			throws IOException, InvalidHandleException, PermissionDeniedException {
		String shandle = handleToString(handle);

		if (openFiles.containsKey(shandle)) {
			OpenFile f = openFiles.get(shandle);

			if(Log.isDebugEnabled())
				Log.debug("Getting file attributes for " + f.getFile().getAbsolutePath());

			return f.getFile().getAttributes();
		}
		throw new InvalidHandleException("The handle is invalid 1");
	}

	public SftpFileAttributes getFileAttributes(String path) throws FileNotFoundException, IOException, PermissionDeniedException {
		return getFileAttributes(path, true);
	}

	public SftpFileAttributes getFileAttributes(String path, boolean followSymlinks)
			throws IOException, FileNotFoundException, PermissionDeniedException {

		if(Log.isDebugEnabled())
			Log.debug("Getting file attributes for " + path);
		AbstractFile f = resolveFile(path, con);
		if(followSymlinks)
			return f.getAttributes();
		else
			return f.getAttributesNoFollowLinks();
	}

	public byte[] openDirectory(String path) throws PermissionDeniedException, FileNotFoundException, IOException {
		return openDirectory(path, null);
	}
	
	public byte[] openDirectory(String path, SftpFileFilter filter) throws PermissionDeniedException, FileNotFoundException, IOException {

		if (Log.isDebugEnabled())
			Log.debug("Opening directory for " + path);

		AbstractFile f = resolveFile(path, con);

		if (!f.isReadable()) {
			throw new PermissionDeniedException("The user does not have permission to read " + path);
		}
		
		if (f.exists()) {
			if (f.isDirectory()) {
				byte[] handle = createHandle();
				openDirectories.put(handleToString(handle), new OpenDirectory(f, filter));
				return handle;
			}

			throw new IOException(path + " is not a directory");
		} else
			throw new FileNotFoundException(path + " does not exist");

	}

	private byte[] createHandle() throws UnsupportedEncodingException, IOException, SshIOException {
		return UUID.randomUUID().toString().getBytes("UTF-8");
	}

	public byte[] stringToHandle(String handle) {
		try {
			return handle.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Your system appears not to support UTF-8!");
		}
	}
	
	public String handleToString(byte[] b) {
		try {
			return new String(b, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Your system appears not to support UTF-8!");
		}
		
	}

	public SftpFile[] readDirectory(byte[] handle)
			throws InvalidHandleException, EOFException, IOException, PermissionDeniedException {

		String shandle = handleToString(handle);

		if (openDirectories.containsKey(shandle)) {
			OpenDirectory dir = openDirectories.get(shandle);

			if (Log.isDebugEnabled())
				Log.debug("Read directory for " + dir.getFile().getAbsolutePath());

			int pos = dir.getPosition();
			AbstractFile[] children = dir.getChildren();
			
			if (dir.children == null) {
				throw new IOException("Permission denined.");
			}

			Vector<SftpFile> files = new Vector<SftpFile>();
			while(files.size() < 100 && pos < children.length) {
				AbstractFile f = children[pos++];
				if(dir.getFilter()==null || dir.getFilter().matches(f.getName())) {
					try {
						SftpFile sftpfile = new SftpFile(f.getName(), f.getAttributes());
						files.add(sftpfile);
					} catch(IOException | PermissionDeniedException e) {
						Log.debug("Could not access attributes of file {}", e, f.getName());
					}
				}
			}
			
			dir.readpos = pos;
			
			if(files.size() > 0) {
				SftpFile[] sf = new SftpFile[files.size()];
				files.copyInto(sf);
				return sf;
			} else {
				throw new EOFException("There are no more files");
			}
		}

		throw new InvalidHandleException("Handle is not an open directory");

	}

	@Deprecated(since = "3.1.0")
	public byte[] openFile(String path, UnsignedInteger32 flags, SftpFileAttributes attrs)
			throws PermissionDeniedException, FileNotFoundException, IOException {
		return openFile(path, flags, Optional.empty(), attrs);
	}

	public byte[] openFile(String path, UnsignedInteger32 flags, Optional<UnsignedInteger32> accessFlags, SftpFileAttributes attrs)
			throws PermissionDeniedException, FileNotFoundException, IOException {

		if(Log.isDebugEnabled())
			Log.debug("Opening file for " + path);

		AbstractFile f = resolveFile(path, con);

		if(f.isDirectory()) {
			throw new PermissionDeniedException("File cannot be opened as it is a Directory");
		}

		if(!(f instanceof NioFile)) {
			/* NioFile uses File.newFileChannel which does these checks itself. The
			 * below method can be removed when the alternatives to NioFile are removed.
			 */
			checkOpenFlagsAndFileState(path, flags, f);
		}

		// Record the open file
		byte[] handle = createHandle();
		openFiles.put(handleToString(handle), f.open(flags, accessFlags, handle));
		
		// Return the handle
		return handle;
	}

	@Deprecated
	protected void checkOpenFlagsAndFileState(String path, UnsignedInteger32 flags, AbstractFile f)
			throws IOException, PermissionDeniedException, FileNotFoundException {
		// Check if the file does not exist and process according to flags
		if (f.exists() && !f.isReadable() && (flags.intValue() & AbstractFileSystem.OPEN_READ) != 0) {
			throw new PermissionDeniedException("The user does not have permission to read.");
		}

		if (((flags.intValue() & AbstractFileSystem.OPEN_WRITE) != 0
				|| (flags.longValue() & AbstractFileSystem.OPEN_CREATE) != 0) && !f.isWritable()) {
			throw new PermissionDeniedException("The user does not have permission to write/create.");
		}
		
		if (!f.exists()) {
			if ((flags.longValue() & AbstractFileSystem.OPEN_CREATE) == AbstractFileSystem.OPEN_CREATE) {
				// The file does not exist and the create flag is present so
				// lets create it
				if (!f.createNewFile()) {
					throw new IOException(path + " could not be created");
				}
			} else {
				// The file does not exist and no create flag present
				throw new FileNotFoundException(path + " does not exist");
			}
		} else {
			if (((flags.longValue() & AbstractFileSystem.OPEN_CREATE) == AbstractFileSystem.OPEN_CREATE)
					&& ((flags.longValue() & AbstractFileSystem.OPEN_EXCLUSIVE) == AbstractFileSystem.OPEN_EXCLUSIVE)) {
				// The file exists but the EXCL flag is set which requires that
				// the
				// file should not exist prior to creation, so throw a status
				// exception
				throw new IOException(path + " already exists");
			}
		}

		// Determine whether we need to truncate the file
		if (((flags.longValue() & AbstractFileSystem.OPEN_CREATE) == AbstractFileSystem.OPEN_CREATE)
				&& ((flags.longValue() & AbstractFileSystem.OPEN_TRUNCATE) == AbstractFileSystem.OPEN_TRUNCATE)) {
			// Set the length to zero
			f.truncate();
		}
	}

	public int readFile(byte[] handle, UnsignedInteger64 offset, byte[] buf, int start, int numBytesToRead)
			throws InvalidHandleException, EOFException, IOException, PermissionDeniedException {
		String shandle = handleToString(handle);

		if (openFiles.containsKey(shandle)) {
			OpenFile file = openFiles.get(shandle);

			if(file.getAccessFlags().isPresent()) {
				var accessFlag = file.getAccessFlags().get().intValue(); 
				if ((accessFlag & ACL.ACE4_READ_DATA) != 0) {
					if (!file.isTextMode() && file.getFilePointer() != offset.longValue()) {
						file.seek(offset.longValue());
					}

					int read = file.read(buf, start, numBytesToRead);

					if (read >= 0) {
						return read;
					}
					return -1;
				}  else {
					throw new InvalidHandleException("The file was not opened for writing");
				}
			}
			else if ((file.getFlags().longValue() & AbstractFileSystem.OPEN_READ) == AbstractFileSystem.OPEN_READ) {

				if (!file.isTextMode() && file.getFilePointer() != offset.longValue()) {
					file.seek(offset.longValue());
				}

				int read = file.read(buf, start, numBytesToRead);

				if (read >= 0) {
					return read;
				}
				return -1;
			}
			throw new InvalidHandleException("The file handle was not opened for reading");

		}
		throw new InvalidHandleException("The handle is invalid 2");
	}

	public void writeFile(byte[] handle, UnsignedInteger64 offset, byte[] data, int off, int len)
			throws InvalidHandleException, IOException, PermissionDeniedException {
		String shandle = handleToString(handle);

		if (openFiles.containsKey(shandle)) {
			OpenFile file = openFiles.get(shandle);

			if(file.getAccessFlags().isPresent()) {
				var accessFlag = file.getAccessFlags().get().intValue(); 
				if ((accessFlag & ACL.ACE4_APPEND_DATA) != 0) {
					// Force the data to be written to the end of the file
					// by seeking to the end
					file.seek(file.getFile().length());
				} else if ((accessFlag & ACL.ACE4_WRITE_DATA) != 0) {
					// Move the file pointer if its not in the write place
					if(!file.isTextMode() && file.getFilePointer() != offset.longValue())
						 file.seek(offset.longValue());
				} else {
					throw new InvalidHandleException("The file was not opened for writing");
				}

				file.write(data, off, len);
			}
			else if ((file.getFlags().longValue() & AbstractFileSystem.OPEN_WRITE) == AbstractFileSystem.OPEN_WRITE) {

				if ((file.getFlags().longValue() & AbstractFileSystem.OPEN_APPEND) == AbstractFileSystem.OPEN_APPEND) {
					// Force the data to be written to the end of the file
					// by seeking to the end
					file.seek(file.getFile().length());
				} else if (!file.isTextMode() && file.getFilePointer() != offset.longValue()) {
					// Move the file pointer if its not in the write place
					file.seek(offset.longValue());
				}

				file.write(data, off, len);
			} else {
				throw new InvalidHandleException("The file was not opened for writing");
			}

		} else {
			throw new InvalidHandleException("The handle is invalid 3");
		}
	}

	public void closeFile(byte[] handle) throws InvalidHandleException, IOException {
		closeFile(handleToString(handle));
	}
//
//	public boolean closeFile(byte[] handle, boolean remove) throws InvalidHandleException, IOException {
//		return closeFile(getHandle(handle), remove);
//	}

	public void freeHandle(byte[] handle) {
		if(handle==null) {
			return;
		}
		String h = handleToString(handle);
		OpenDirectory dir = openDirectories.get(h);
		if(dir!=null) {
			openDirectories.remove(h);
		} else {
			openFiles.remove(h);

		}
	}
	
	public void closeFile(String handle) throws InvalidHandleException, IOException {
		
		OpenDirectory dir = openDirectories.get(handle);
		if(dir!=null)  {
			// We don't close a directory as there is no resource attached to it
			return;
		}
		
		OpenFile file = openFiles.get(handle);
		if(file==null) {
			throw new InvalidHandleException(handle + " is an invalid handle");
		}
		
		file.close();
		
	}

	public FileVolume getVolume(String path) throws IOException, PermissionDeniedException {
		AbstractFile f = resolveFile(path, con);
		if (f.exists()) {
			return f.getVolume();
		} else {
			throw new FileNotFoundException(path + " does not exist");
		}
	}

	public void removeFile(String path) throws PermissionDeniedException, IOException, FileNotFoundException {

		AbstractFile f = resolveFile(path, con);

		if (!f.isWritable()) {
			throw new PermissionDeniedException("User does not have the permission to delete.");
		}

		if (f.existsNoFollowLinks()) {
			try {
				if (!f.isDirectory()) {
					if (!f.delete(false)) {
						throw new IOException("Failed to delete " + path);
					}
				} else {
					throw new FileIsDirectoryException(path + " is a directory, use remove directory command to remove");
				}
			} catch (SecurityException se) {
				throw new PermissionDeniedException("Permission denied");
			}
		} else {
			throw new FileNotFoundException(path + " does not exist");
		}
	}

	public void renameFile(String oldpath, String newpath)
			throws PermissionDeniedException, FileNotFoundException, IOException {

		AbstractFile f1 = fileFactory.getFile(oldpath);
		AbstractFile f2 = fileFactory.getFile(newpath);

		if (!f1.isWritable()) {
			throw new PermissionDeniedException("User does not have permission to change " + oldpath);
		}

		if (!f2.isWritable()) {
			throw new PermissionDeniedException("User does not have permission to write " + newpath);
		}

		if (f1.exists()) {
			if (!f2.exists()) {
				f1.moveTo(f2);
			} else {
				if(f2.isDirectory() && Boolean.getBoolean("maverick.enableRenameIntoDir")) {
					f2 = fileFactory.getFile(FileUtils.checkEndsWithSlash(f2.getAbsolutePath()) + f1.getName());
					if(f2.exists()) {
						throw new IOException(newpath + " already exists");
					}
					f1.moveTo(f2);
				} else {
					throw new IOException(newpath + " already exists");
				}
			}
		} else {
			throw new FileNotFoundException(oldpath + " does not exist");
		}
	}
	
	public void copyFile(String oldpath, String newpath, boolean overwrite) 
			throws PermissionDeniedException, FileNotFoundException, FileAlreadyExistsException, IOException {
		
		AbstractFile f1 = fileFactory.getFile(oldpath);
		AbstractFile f2 = fileFactory.getFile(newpath);

		if(!f1.exists()) {
			throw new FileNotFoundException(oldpath + " does not exist");
		}
		
		if(f2.exists() && f2.isDirectory()) {
			f2 = fileFactory.getFile(FileUtils.checkEndsWithSlash(f2.getAbsolutePath()) + f1.getName());
		}
		
		if(f2.exists() && !overwrite) {
			throw new FileAlreadyExistsException(newpath);
		}

		if (f2.exists() && !f2.isWritable()) {
			throw new PermissionDeniedException("User does not have permission to write " + newpath);
		}

		f2.copyFrom(f1);
	}

	public String getDefaultPath() throws IOException, PermissionDeniedException {
		return fileFactory.getDefaultPath().getCanonicalPath();
	}

	public void removeDirectory(String path) throws PermissionDeniedException, FileNotFoundException, IOException {

		AbstractFile f = resolveFile(path, con);

		if (!f.isWritable()) {
			throw new PermissionDeniedException("User does not have the permission to write.");
		}

		if (f.isDirectory()) {
			if (f.exists()) {
				if (f.getChildren().size() == 0) {
					if (!f.delete(false)) {
						throw new IOException("Failed to remove directory " + path);
					}
				} else {
					throw new IOException(path + " is not an empty directory");
				}
			} else {
				throw new FileNotFoundException(path + " does not exist");
			}
		} else {
			throw new IOException(path + " is not a directory");
		}
	}

	public void setFileAttributes(String path, SftpFileAttributes attrs)
			throws PermissionDeniedException, IOException, FileNotFoundException {

		AbstractFile f = resolveFile(path, con);
		f.setAttributes(attrs);
	}

	public void setFileAttributes(byte[] handle, SftpFileAttributes attrs)
			throws PermissionDeniedException, IOException, InvalidHandleException {

		String shandle = handleToString(handle);
		if (openFiles.containsKey(shandle)) {
			OpenFile f = openFiles.get(shandle);
			f.getFile().setAttributes(attrs);
		} else if (openDirectories.containsKey(shandle)) {
			OpenDirectory dir = openDirectories.get(shandle);
			dir.getFile().setAttributes(attrs);
		} else
			throw new InvalidHandleException(shandle);
	}

	public SftpFile readSymbolicLink(String link)
			throws UnsupportedFileOperationException, FileNotFoundException, IOException, PermissionDeniedException {
		try {
			String linkTarget = resolveFile(link, con).readSymbolicLink();
			AbstractFile f = resolveFile(linkTarget, con);
			SftpFileAttributes a = f.getAttributes();
			return new SftpFile(linkTarget, a);
		}
		catch(UnsupportedOperationException uoe) {
			throw new UnsupportedFileOperationException("Symbolic links are not supported by the Virtual File System");
		}		
	}

	@SuppressWarnings("removal")
	public void createSymbolicLink(String link, String target)
			throws UnsupportedFileOperationException, FileNotFoundException, IOException, PermissionDeniedException {
		try {
			resolveFile(target, con).symlinkFrom(link);
		}
		catch(UnsupportedOperationException uoe) {
			try {
				resolveFile(link, con).symlinkTo(target);
			}
			catch(UnsupportedOperationException uoe2) {
				throw new UnsupportedFileOperationException("Symbolic links are not supported by the Virtual File System");
			}
		}
	}

	@SuppressWarnings("removal")
	public void createLink(String link, String target)
			throws UnsupportedFileOperationException, FileNotFoundException, IOException, PermissionDeniedException {
		try {
			resolveFile(target, con).linkFrom(link);
		}
		catch(UnsupportedOperationException uoe) {
			try {
				resolveFile(link, con).linkTo(target);
			}
			catch(UnsupportedOperationException uoe2) {
				throw new UnsupportedFileOperationException("Hard links are not supported by the Virtual File System");
			}
		}
	}

	public boolean fileExists(String path) throws IOException, PermissionDeniedException {
		return fileExists(path, true);
	}

	public boolean fileExists(String path, boolean followLinks) throws IOException, PermissionDeniedException {
		try {
			AbstractFile f = resolveFile(path, con);
			if(followLinks)
				return f.exists();
			else
				return f.existsNoFollowLinks();
		} catch (FileNotFoundException fnfe) {
			// VirtualMappedFile.translateCanonicalPath throws this when it
			// can't translate path. I don't think this is right, but this will
			// do for now
			return false;
		}
	}

	public String getRealPath(String path) throws IOException, FileNotFoundException, PermissionDeniedException {
		AbstractFile f = resolveFile(path, con);
		return f.getCanonicalPath();
	}
	
	public AbstractFile getFileForHandle(byte[] handle) throws IOException, InvalidHandleException {
		
		if(!openFiles.containsKey(handleToString(handle))) {
			throw new InvalidHandleException("Invalid handle passed to getFileForHandle");
		}
		
		return openFiles.get(handleToString(handle)).getFile();
	}

	

	protected class OpenDirectory {
		AbstractFile f;
		AbstractFile[] children;
		int readpos = 0;
		SftpFileFilter filter;

		public OpenDirectory(AbstractFile f, SftpFileFilter filter) throws IOException, PermissionDeniedException {
			this.f = f;
			this.filter = filter;
			this.children = f.getChildren().toArray(new AbstractFile[0]);
		}

		public AbstractFile getFile() {
			return f;
		}

		public AbstractFile[] getChildren() throws IOException, PermissionDeniedException {
			return children;
		}

		public int getPosition() {
			return readpos;
		}
		
		public SftpFileFilter getFilter() {
			return filter;
		}

		public void setPosition(int readpos) {
			this.readpos = readpos;
		}
	}

	public void populateEvent(Event evt) {
		if(Objects.isNull(fileFactory)) {
			return;
		}
		
		evt.addAttribute(EventCodes.ATTRIBUTE_FILE_FACTORY, fileFactory);
		evt.addAttribute(EventCodes.ATTRIBUTE_CONNECTION, con);
		byte[] handle = (byte[]) evt.getAttribute(EventCodes.ATTRIBUTE_HANDLE);
		if(handle!=null) {
			String h = handleToString(handle);
			OpenFile openFile = openFiles.get(h);
			if(openFile!=null) {
				openFile.processEvent(evt);
			} else {
				OpenDirectory openDirectory = openDirectories.get(h);
				if(openDirectory!=null) {
					if(openDirectory.f!=null) {
						evt.addAttribute(EventCodes.ATTRIBUTE_ABSTRACT_FILE, openDirectory.f);
					}
				} 
			}
		}

		fileFactory.populateEvent(evt);
	}

	public SshConnection getConnection() {
		return con;
	}

	public String getPathForHandle(byte[] handle) throws IOException, InvalidHandleException {

		String h = handleToString(handle);

		try {
			if (openFiles.containsKey(h)) {
				return openFiles.get(h).getFile().getAbsolutePath();
			} else if (openDirectories.containsKey(h)) {
				return openDirectories.get(h).getFile().getAbsolutePath();
			}
		} catch (PermissionDeniedException e) {
			Log.error("Permission denied in getPathForHandle!", e);
		}
		throw new InvalidHandleException("Invalid handle");
	}

	
	public void copyData(byte[] handle, UnsignedInteger64 offset, UnsignedInteger64 length, byte[] toHandle,
			UnsignedInteger64 toOffset) throws IOException, PermissionDeniedException, InvalidHandleException {
		
		if(length.longValue() > 0) {
			copyLength(handle, offset, length, toHandle, toOffset);
		} else {
			copyUntilEOF(handle, offset, toHandle, toOffset);
		}			
	}
	
	private void copyUntilEOF(byte[] handle, UnsignedInteger64 offset, byte[] toHandle, UnsignedInteger64 toOffset) throws InvalidHandleException, IOException, PermissionDeniedException {
		
		byte[] buffer = new byte[65525];
		int r;
		long read = 0L;
		do {
			r = readFile(handle, UnsignedInteger64.add(new UnsignedInteger64(read), offset), buffer, 0, buffer.length);
			if(r > -1) {
				writeFile(toHandle, UnsignedInteger64.add(new UnsignedInteger64(read), toOffset), buffer, 0, r);
				read+=r;
			}
		} while(r > -1);
		
	}

	private void copyLength(byte[] handle, UnsignedInteger64 offset, UnsignedInteger64 length, byte[] toHandle, UnsignedInteger64 toOffset) throws EOFException, InvalidHandleException, IOException, PermissionDeniedException {
		
		if(length.longValue() <= 0) {
			throw new IllegalArgumentException("copyLength requires a positive length value");
		}
		byte[] buffer = new byte[65525];
		long read = 0;
		int r;
		do {
			r = readFile(handle, UnsignedInteger64.add(new UnsignedInteger64(read), offset), buffer, 0, (int) Math.min(buffer.length, length.longValue() - read));
			if(r > -1) {
				writeFile(toHandle, UnsignedInteger64.add(new UnsignedInteger64(read), toOffset), buffer, 0, r);
				read += r;
			}
		} while(r > -1 && read < length.longValue());

	}

	public boolean isMultipartTransferSupported(String path) throws PermissionDeniedException, IOException {
		
		AbstractFile f = resolveFile(path, con);
		return f.supportsMultipartTransfers();
	}

	public MultipartTransfer startMultipartUpload(AbstractFile targetFile) throws PermissionDeniedException, IOException {
		
		MultipartTransfer mpt = targetFile.startMultipartUpload(targetFile);
		
		MultipartTransferRegistry.registerTransfer(mpt);
		
		return mpt;
	}

	public byte[] openPart(String uuid, Multipart part) throws IOException, PermissionDeniedException {
		
		MultipartTransfer mpt = MultipartTransferRegistry.getTransfer(uuid);
		
		if(Objects.isNull(mpt)) {
			throw new PermissionDeniedException("Unexpected multipart request for uuid " + uuid);
		}
		
		
		OpenFile file = mpt.openPart(part);
		
		openFiles.put(handleToString(file.getHandle()), file);

		return file.getHandle();
	}
}
