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
/* HEADER */
package com.sshtools.common.sftp;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.files.FileUtils;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshIOException;
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

	public AbstractFileSystem(SshConnection con, String protocolInUse) {
		this.fileFactory = con.getFileFactory();
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
			return fileFactory.getDefaultPath(con);
		} else {
			return fileFactory.getFile(path, con);
		}
	}

	public void closeFilesystem() {
		String obj;
		for (Iterator<String> it = openFiles.keySet().iterator(); it.hasNext();) {
			obj = it.next();
			try {
				closeFile(obj, false);
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
				closeFile(obj, false);
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
		String shandle = getHandle(handle);

		if (openFiles.containsKey(shandle)) {
			OpenFile f = openFiles.get(shandle);

			if(Log.isDebugEnabled())
				Log.debug("Getting file attributes for " + f.getFile().getAbsolutePath());

			return f.getFile().getAttributes();
		}
		throw new InvalidHandleException("The handle is invalid 1");
	}

	public SftpFileAttributes getFileAttributes(String path)
			throws IOException, FileNotFoundException, PermissionDeniedException {

		if(Log.isDebugEnabled())
			Log.debug("Getting file attributes for " + path);
		AbstractFile f = resolveFile(path, con);
		return f.getAttributes();
	}

	public byte[] openDirectory(String path) throws PermissionDeniedException, FileNotFoundException, IOException {

		if(Log.isDebugEnabled())
			Log.debug("Opening directory for " + path);

		AbstractFile f = resolveFile(path, con);

		if (!f.isReadable()) {
			throw new PermissionDeniedException("The user does not have permission to read " + path);
		}
		
		if (f.exists()) {
			if (f.isDirectory()) {
				byte[] handle = createHandle();
				openDirectories.put(getHandle(handle), new OpenDirectory(f));
				return handle;
			}

			throw new IOException(path + " is not a directory");
		} else
			throw new FileNotFoundException(path + " does not exist");

	}

	private byte[] createHandle() throws UnsupportedEncodingException, IOException, SshIOException {
		return UUID.randomUUID().toString().getBytes("UTF-8");
	}

	protected byte[] getHandle(String handle) {
		try {
			return handle.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Your system appears not to support UTF-8!");
		}
	}
	
	protected String getHandle(byte[] b) {
		try {
			return new String(b, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Your system appears not to support UTF-8!");
		}
		
	}

	public SftpFile[] readDirectory(byte[] handle)
			throws InvalidHandleException, EOFException, IOException, PermissionDeniedException {
		String shandle = getHandle(handle);

		if (openDirectories.containsKey(shandle)) {
			OpenDirectory dir = openDirectories.get(shandle);

			if(Log.isDebugEnabled())
				Log.debug("Read directory for " + dir.getFile().getAbsolutePath());

			int pos = dir.getPosition();
			AbstractFile[] children = dir.getChildren();

			if (children == null) {
				throw new IOException("Permission denined.");
			}

			int count = ((children.length - pos) < 100) ? (children.length - pos) : 100;

			if (count > 0) {
				Vector<SftpFile> files = new Vector<SftpFile>();

				for (int i = 0; i < count; i++) {
					AbstractFile f = children[pos + i];
					SftpFile sftpfile = new SftpFile(f.getName(), f.getAttributes());
					files.add(sftpfile);
				}

				dir.readpos = pos + files.size();
				SftpFile[] sf = new SftpFile[files.size()];
				files.copyInto(sf);
				return sf;
			}

			throw new EOFException("There are no more files");
		}

		throw new InvalidHandleException("Handle is not an open directory");

	}

	public byte[] openFile(String path, UnsignedInteger32 flags, SftpFileAttributes attrs)
			throws PermissionDeniedException, FileNotFoundException, IOException {

		if(Log.isDebugEnabled())
			Log.debug("Opening file for " + path);

		AbstractFile f = resolveFile(path, con);

		if(f.isDirectory()) {
			throw new PermissionDeniedException("File cannot be opened as it is a Directory");
		}
		
		if (f.exists() && !f.isReadable() && (flags.intValue() & AbstractFileSystem.OPEN_READ) != 0) {
			throw new PermissionDeniedException("The user does not have permission to read.");
		}

		if (((flags.intValue() & AbstractFileSystem.OPEN_WRITE) != 0
				|| (flags.longValue() & AbstractFileSystem.OPEN_CREATE) != 0) && !f.isWritable()) {
			throw new PermissionDeniedException("The user does not have permission to write/create.");
		}

		// Check if the file does not exist and process according to flags
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

		// Record the open file
		byte[] handle = createHandle();
		openFiles.put(getHandle(handle), new OpenFile(f, flags));

		// Return the handle
		return handle;
	}

	public int readFile(byte[] handle, UnsignedInteger64 offset, byte[] buf, int start, int numBytesToRead)
			throws InvalidHandleException, EOFException, IOException {
		String shandle = getHandle(handle);

		if (openFiles.containsKey(shandle)) {
			OpenFile file = openFiles.get(shandle);

			if ((file.getFlags().longValue() & AbstractFileSystem.OPEN_READ) == AbstractFileSystem.OPEN_READ) {

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
			throws InvalidHandleException, IOException {
		String shandle = getHandle(handle);

		if (openFiles.containsKey(shandle)) {
			OpenFile file = openFiles.get(shandle);

			if ((file.getFlags().longValue() & AbstractFileSystem.OPEN_WRITE) == AbstractFileSystem.OPEN_WRITE) {

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
		closeFile(handle, true);
	}

	public boolean closeFile(byte[] handle, boolean remove) throws InvalidHandleException, IOException {
		return closeFile(getHandle(handle), remove);
	}

	protected boolean closeFile(String handle, boolean remove) throws InvalidHandleException, IOException {
		
		OpenDirectory dir = openDirectories.get(handle);
		if(dir!=null) {
			openDirectories.remove(handle);
		} else {
			OpenFile file = openFiles.get(handle);
			if(file==null) {
				throw new InvalidHandleException(handle + " is an invalid handle");
			}
			
			file.close();
			if(remove) {
				openFiles.remove(handle);
			} else {
				return true;
			}	
		}
		return false;
	}

	public void removeFile(String path) throws PermissionDeniedException, IOException, FileNotFoundException {

		AbstractFile f = resolveFile(path, con);

		if (!f.isWritable()) {
			throw new PermissionDeniedException("User does not have the permission to delete.");
		}

		if (f.exists()) {
			try {
				if (f.isFile()) {
					if (!f.delete(false)) {
						throw new IOException("Failed to delete " + path);
					}
				} else {
					throw new IOException(path + " is a directory, use remove directory command to remove");
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

		AbstractFile f1 = fileFactory.getFile(oldpath, con);
		AbstractFile f2 = fileFactory.getFile(newpath, con);

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
					f2 = fileFactory.getFile(FileUtils.checkEndsWithSlash(f2.getAbsolutePath()) + f1.getName(), con);
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

	public String getDefaultPath() throws IOException, PermissionDeniedException {
		return fileFactory.getDefaultPath(con).getCanonicalPath();
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

		String shandle = getHandle(handle);
		if (openFiles.containsKey(shandle)) {
			OpenFile f = openFiles.get(shandle);
			f.getFile().setAttributes(attrs);
		} else if (openDirectories.containsKey(shandle)) {
			OpenDirectory dir = openDirectories.get(shandle);
			dir.getFile().setAttributes(attrs);
		} else
			throw new InvalidHandleException(shandle);
	}

	public SftpFile readSymbolicLink(String path)
			throws UnsupportedFileOperationException, FileNotFoundException, IOException, PermissionDeniedException {
		throw new UnsupportedFileOperationException("Symbolic links are not supported by the Virtual File System");
	}

	public void createSymbolicLink(String link, String target)
			throws UnsupportedFileOperationException, FileNotFoundException, IOException, PermissionDeniedException {
		throw new UnsupportedFileOperationException("Symbolic links are not supported by the Virtual File System");
	}

	public boolean fileExists(String path) throws IOException, PermissionDeniedException {
		try {
			AbstractFile f = resolveFile(path, con);
			return f.exists();
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
		
		if(!openFiles.containsKey(getHandle(handle))) {
			throw new InvalidHandleException("Invalid handle passed to getFileForHandle");
		}
		
		return openFiles.get(getHandle(handle)).getFile();
	}

	protected class OpenFile {
		AbstractFile f;
		UnsignedInteger32 flags;
		long filePointer;
		boolean textMode = false;
		InputStream in;
		OutputStream out;
		AbstractFileRandomAccess raf;
		boolean closed;

		public OpenFile(AbstractFile f, UnsignedInteger32 flags) throws IOException {
			this.f = f;
			this.flags = flags;
			if (f.supportsRandomAccess()) {
				raf = f.openFile(((flags.intValue() & AbstractFileSystem.OPEN_WRITE) != 0));
			}
			this.textMode = (flags.intValue() & AbstractFileSystem.OPEN_TEXT) != 0;
			if (isTextMode() && Log.isDebugEnabled()) {
				Log.debug(f.getName() + " is being opened in TEXT mode");
			}
		}

		public boolean isTextMode() {
			return textMode;
		}

		public void close() throws IOException {
			if (in != null) {
				try {
					in.close();
				} finally {
					in = null;
				}
			}
			if (out != null) {
				try {
					out.close();
				} finally {
					out = null;
				}
			}
			if (raf != null) {
				try {
					raf.close();
				} finally {
					raf = null;
				}
			}
			closed = true;
		}

		public int read(byte[] buf, int off, int len) throws IOException {
			if(closed) {
				return -1;
			}
			if (raf == null) {
				if (filePointer == -1)
					return -1;
				InputStream in = getInputStream();
				int count = 0;
				while (count < len) {
					int r = in.read(buf, off + count, len - count);
					if (r == -1) {
						if (count == 0) {
							filePointer = -1;
							return -1;
						} else {
							return count;
						}
					} else {
						filePointer += r;
						count += r;
					}
				}
				return count;
			} else {
				return raf.read(buf, off, len);
			}
		}

		public void write(byte[] buf, int off, int len) throws IOException {
			if(closed) {
				throw new IOException("File has been closed.");
			}
			if (raf == null) {
				if (filePointer == -1)
					throw new IOException("File is EOF");
				OutputStream out = getOutputStream();
				out.write(buf, off, len);
				filePointer += len;
			} else {
				raf.write(buf, off, len);
			}
		}

		private OutputStream getOutputStream() throws IOException {
			if(closed) {
				throw new IOException("File has been closed [getOutputStream].");
			}
			if (out == null)
				out = f.getOutputStream();
			return out;
		}

		private InputStream getInputStream() throws IOException {
			if(closed) {
				throw new IOException("File has been closed [getInputStream].");
			}
			if (in == null)
				in = f.getInputStream();
			return in;
		}

		public void seek(long longValue) throws IOException {
			if(closed) {
				throw new IOException("File has been closed [getOutputStream].");
			}
			if (raf == null) {
				filePointer = -1;
				return;
			}
			raf.seek(longValue);
		}

		public AbstractFile getFile() {
			return f;
		}

		public UnsignedInteger32 getFlags() {
			return flags;
		}

		public long getFilePointer() throws IOException {
			if(closed) {
				throw new IOException("File has been closed [getFilePointer].");
			}
			return raf == null ? filePointer : raf.getFilePointer();
		}
	}

	protected class OpenDirectory {
		AbstractFile f;
		AbstractFile[] children;
		int readpos = 0;

		public OpenDirectory(AbstractFile f) throws IOException, PermissionDeniedException {
			this.f = f;
			this.children = f.getChildren().toArray(new AbstractFile[0]);
		}

		public AbstractFile getFile() {
			return f;
		}

		public AbstractFile[] getChildren() {
			return children;
		}

		public int getPosition() {
			return readpos;
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
			
			OpenFile openFile = openFiles.get(getHandle(handle));
			if(openFile!=null) {
				if(openFile.f!=null) {
					evt.addAttribute(EventCodes.ATTRIBUTE_ABSTRACT_FILE, openFile.f);
				}
				if(openFile.in!=null) {
					evt.addAttribute(EventCodes.ATTRIBUTE_ABSTRACT_FILE_INPUTSTREAM, openFile.in);
				}
				if(openFile.out!=null) {
					evt.addAttribute(EventCodes.ATTRIBUTE_ABSTRACT_FILE_OUTPUTSTREAM, openFile.out);
				}
				if(openFile.raf!=null) {
					evt.addAttribute(EventCodes.ATTRIBUTE_ABSTRACT_FILE_RANDOM_ACCESS, openFile.raf);
				}
			}
		}
		fileFactory.populateEvent(evt);
	}

	public SshConnection getConnection() {
		return con;
	}

	public String getPathForHandle(byte[] handle) throws IOException, InvalidHandleException {

		String h = getHandle(handle);

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

}
