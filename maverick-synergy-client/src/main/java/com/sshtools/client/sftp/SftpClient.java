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

package com.sshtools.client.sftp;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import com.sshtools.client.SshClient;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.direct.DirectFileFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.GlobSftpFileFilter;
import com.sshtools.common.sftp.RegexSftpFileFilter;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpFileFilter;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.EOLProcessor;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;
import com.sshtools.common.util.Utils;

/**
 * An abstract task that implements an SFTP client.
 */
public class SftpClient implements Closeable {

	SftpChannel sftp;

	String cwd;
	AbstractFile lcwd;
	AbstractFileFactory<?> fileFactory;

	private int blocksize = -1;
	private int asyncRequests = -1;
	private int buffersize = 1024000;

	// Default permissions is determined by default_permissions ^ umask
	int umask = 0022;
	boolean applyUmask = false;

	/**
	 * Instructs the client to use a binary transfer mode when used with
	 * {@link setTransferMode(int)}
	 */
	public static final int MODE_BINARY = 1;

	/**
	 * Instructs the client to use a text transfer mode when used with
	 * {@link setTransferMode(int)}.
	 */
	public static final int MODE_TEXT = 2;

	/**
	 * <p>
	 * Specifies that the remote server is using \r\n as its newline convention when
	 * used with {@link setRemoteEOL(int)}
	 * </p>
	 */
	public static final int EOL_CRLF = EOLProcessor.TEXT_CRLF;

	/**
	 * <p>
	 * Specifies that the remote server is using \n as its newline convention when
	 * used with {@link setRemoteEOL(int)}
	 * </p>
	 */
	public static final int EOL_LF = EOLProcessor.TEXT_LF;

	/**
	 * <p>
	 * Specifies that the remote server is using \r as its newline convention when
	 * used with {@link setRemoteEOL(int)}
	 * </p>
	 */
	public static final int EOL_CR = EOLProcessor.TEXT_CR;

	private int outputEOL = EOL_CRLF;
	private int inputEOL = EOLProcessor.TEXT_SYSTEM;
	private boolean stripEOL = false;
	private boolean forceRemoteEOL;

	private int transferMode = MODE_BINARY;

	public SftpClient(SshConnection con) throws SshException, PermissionDeniedException, IOException {
		this(con, new DirectFileFactory(new java.io.File(System.getProperty("user.home"))));

	}

	public SftpClient(SshConnection con, AbstractFileFactory<?> fileFactory)
			throws PermissionDeniedException, IOException, SshException {
		this.fileFactory = fileFactory;
		this.cwd = "";
		this.lcwd = fileFactory.getFile("");
		this.sftp = new SftpChannel(con);
	}

	public SftpClient(SshClient ssh) throws SshException, PermissionDeniedException, IOException {
		this(ssh.getConnection());
	}

	public SftpClient(SshClient ssh, AbstractFileFactory<?> fileFactory)
			throws SshException, PermissionDeniedException, IOException {
		this(ssh.getConnection(), fileFactory);
	}

	/**
	 * Sets the block size used when transferring files, defaults to the optimized
	 * setting of 32768. You should not increase this value as the remote server may
	 * not be able to support higher blocksizes.
	 * 
	 * @param blocksize
	 */
	public void setBlockSize(int blocksize) {
		if (blocksize < 512) {
			throw new IllegalArgumentException("Block size must be greater than 512");
		}
		this.blocksize = blocksize;
	}

	/**
	 * Returns the instance of the AbstractSftpChannel used by this class
	 * 
	 * @return the AbstractSftpChannel instance
	 */
	public SftpChannel getSubsystemChannel() {
		return sftp;
	}

	/**
	 * <p>
	 * Sets the transfer mode for current operations. The valid modes are:<br>
	 * <br>
	 * {@link #MODE_BINARY} - Files are transferred in binary mode and no processing
	 * of text files is performed (default mode).<br>
	 * <br>
	 * {@link #MODE_TEXT} - For servers supporting version 4+ of the SFTP protocol
	 * files are transferred in text mode. For earlier protocol versions the files
	 * are transfered in binary mode but the client performs processing of text; if
	 * files are written to the remote server the client ensures that the line
	 * endings conform to the remote EOL mode set using {@link setRemoteEOL(int)}.
	 * For files retrieved from the server the EOL policy is based upon System
	 * policy as defined by the "line.seperator" system property.
	 * </p>
	 * 
	 * @param transferMode int
	 */
	public void setTransferMode(int transferMode) {
		if (transferMode != MODE_BINARY && transferMode != MODE_TEXT)
			throw new IllegalArgumentException("Mode can only be either binary or text");

		this.transferMode = transferMode;

		if (Log.isDebugEnabled())
			Log.debug("Transfer mode set to " + (transferMode == MODE_BINARY ? "binary" : "text"));

	}

	/**
	 * Strip all line endings in preference of the target system EOL setting.
	 * 
	 * @param stripEOL
	 */
	public void setStripEOL(boolean stripEOL) {
		this.stripEOL = stripEOL;
	}

	/**
	 * <p>
	 * When connected to servers running SFTP version 3 (or less) the remote EOL
	 * type needs to be explicitly set because there is no reliable way for the
	 * client to determine the type of EOL for text files. In versions 4+ a
	 * mechanism is provided and this setting is overridden.
	 * </p>
	 * 
	 * <p>
	 * Valid values for this method are {@link EOL_CRLF} (default), {@link EOL_CR},
	 * and {@link EOL_LF}.
	 * </p>
	 * 
	 * @param eolMode int
	 */
	public void setRemoteEOL(int eolMode) {
		this.outputEOL = eolMode;

		if (Log.isDebugEnabled())
			Log.debug("Remote EOL set to " + (eolMode == EOL_CRLF ? "CRLF" : (eolMode == EOL_CR ? "CR" : "LF")));

	}

	/**
	 * <p>
	 * Override the default local system EOL for text mode files.
	 * </p>
	 * 
	 * <p>
	 * Valid values for this method are {@link EOL_CRLF} (default), {@link EOL_CR},
	 * and {@link EOL_LF}.
	 * </p>
	 * 
	 * @param eolMode int
	 */
	public void setLocalEOL(int eolMode) {
		this.inputEOL = eolMode;

		if (Log.isDebugEnabled())
			Log.debug("Input EOL set to " + (eolMode == EOL_CRLF ? "CRLF" : (eolMode == EOL_CR ? "CR" : "LF")));

	}

	/**
	 * Override automatic detection of the remote EOL (any SFTP version). USE WITH
	 * CAUTION.
	 * 
	 * @param forceRemoteEOL
	 */
	public void setForceRemoteEOL(boolean forceRemoteEOL) {
		this.forceRemoteEOL = forceRemoteEOL;
	}

	/**
	 * 
	 * @return int
	 */
	public int getTransferMode() {
		return transferMode;
	}

	/**
	 * Set the size of the buffer which is used to read from the local file system.
	 * This setting is used to optimize the writing of files by allowing for a large
	 * chunk of data to be read in one operation from a local file. The previous
	 * version simply read each block of data before sending however this decreased
	 * performance, this version now reads the file into a temporary buffer in order
	 * to reduce the number of local filesystem reads. This increases performance
	 * and so this setting should be set to the highest value possible. The default
	 * setting is negative which means the entire file will be read into a temporary
	 * buffer.
	 * 
	 * @param buffersize
	 */
	public void setBufferSize(int buffersize) {
		this.buffersize = buffersize;

		if (Log.isDebugEnabled())
			Log.debug("Buffer size set to " + buffersize);

	}

	/**
	 * Set the maximum number of asynchronous requests that are outstanding at any
	 * one time. This setting is used to optimize the reading and writing of files
	 * to/from the remote file system when using the get and put methods. The
	 * default for this setting is 100.
	 * 
	 * @param asyncRequests
	 */
	public void setMaxAsyncRequests(int asyncRequests) {
		if (asyncRequests < 1) {
			throw new IllegalArgumentException("Maximum asynchronous requests must be greater or equal to 1");
		}
		this.asyncRequests = asyncRequests;

		if (Log.isDebugEnabled())
			Log.debug("Max async requests set to " + asyncRequests);

	}

	/**
	 * Sets the umask used by this client. <blockquote>
	 * 
	 * <pre>
	 * To give yourself full permissions for both files and directories and
	 * prevent the group and other users from having access:
	 * 
	 *   umask(077);
	 * 
	 * This subtracts 077 from the system defaults for files and directories
	 * 666 and 777. Giving a default access permissions for your files of
	 * 600 (rw-------) and for directories of 700 (rwx------).
	 * 
	 * To give all access permissions to the group and allow other users read
	 * and execute permission:
	 * 
	 *   umask(002);
	 * 
	 * This subtracts 002 from the system defaults to give a default access permission
	 * for your files of 664 (rw-rw-r--) and for your directories of 775 (rwxrwxr-x).
	 * 
	 * To give the group and other users all access except write access:
	 * 
	 *   umask(022);
	 * 
	 * This subtracts 022 from the system defaults to give a default access permission
	 * for your files of 644 (rw-r--r--) and for your directories of 755 (rwxr-xr-x).
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @param umask
	 * @return the previous umask value
	 */
	public int umask(int umask) {
		applyUmask = true;
		int old = this.umask;
		this.umask = umask;

		if (Log.isDebugEnabled())
			Log.debug("umask " + umask);

		return old;
	}

	public SftpFile openFile(String fileName) throws SftpStatusException, SshException {
		return openFile(fileName, SftpChannel.OPEN_READ);
	}

	public SftpFile openFile(String fileName, int flags) throws SftpStatusException, SshException {
		if (transferMode == MODE_TEXT && sftp.getVersion() > 3) {
			return sftp.openFile(resolveRemotePath(fileName), flags | SftpChannel.OPEN_TEXT);
		}
		return sftp.openFile(resolveRemotePath(fileName), flags);
	}

	public SftpFile openDirectory(String path) throws SftpStatusException, SshException {
		return sftp.openDirectory(path);
	}

	public List<SftpFile> readDirectory(SftpFile dir) throws SftpStatusException, SshException {
		List<SftpFile> results = new ArrayList<>();
		if (sftp.listChildren(dir, results) == -1) {
			return null;
		}
		return results;
	}

	/**
	 * <p>
	 * Changes the working directory on the remote server, or the user's default
	 * directory if <code>null</code> or any empty string is provided as the
	 * directory path. The user's default directory is typically their home
	 * directory but is dependent upon server implementation.
	 * </p>
	 * 
	 * @param dir the new working directory
	 * 
	 * @throws IOException         if an IO error occurs or the file does not exist
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void cd(String dir) throws SftpStatusException, SshException {
		String actual;

		if (dir == null || dir.equals("")) {
			actual = sftp.getDefaultDirectory();
		} else {
			actual = resolveRemotePath(dir);
			actual = sftp.getAbsolutePath(actual);
		}

		if (!actual.equals("")) {
			SftpFileAttributes attr = sftp.getAttributes(actual);

			if (!attr.isDirectory()) {
				throw new SftpStatusException(SftpStatusException.SSH_FX_FAILURE, dir + " is not a directory");
			}
		}

		if (Log.isDebugEnabled())
			Log.debug("Changing dir from " + cwd + " to " + (actual.equals("") ? "user default dir" : actual));

		cwd = actual;
	}

	/**
	 * <p>
	 * Get the default directory (or HOME directory)
	 * </p>
	 * 
	 * @return String
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public String getDefaultDirectory() throws SftpStatusException, SshException {
		return sftp.getDefaultDirectory();
	}

	/**
	 * Change the working directory to the parent directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void cdup() throws SftpStatusException, SshException {

		SftpFile cd = sftp.getFile(cwd);

		SftpFile parent = cd.getParent();

		if (parent != null)
			cwd = parent.getAbsolutePath();

	}

	private AbstractFile resolveLocalPath(String path) throws IOException, PermissionDeniedException {
		return lcwd.resolveFile(path);
	}

	private boolean isWindowsRoot(String path) {

		// true if path>2 and starts with a letter followed by a ':' followed by
		// '/' or '\\'
		return path.length() > 2 && (((path.charAt(0) >= 'a' && path.charAt(0) <= 'z')
				|| (path.charAt(0) >= 'A' && path.charAt(0) <= 'Z')) && path.charAt(1) == ':' && path.charAt(2) == '/'
				|| path.charAt(2) == '\\');
	}

	/**
	 * some devices have unusual file system roots such as "flash:", customRoots
	 * contains these. If a device uses roots like this, and folder traversal on the
	 * device is required then it must have its root stored in customRoots
	 */
	private Vector<String> customRoots = new Vector<String>();

	/**
	 * Add a custom file system root path such as "flash:"
	 * 
	 * @param rootPath
	 */
	public void addCustomRoot(String rootPath) {
		customRoots.addElement(rootPath);
	}

	/**
	 * Remove a custom file system root path such as "flash:"
	 * 
	 * @param rootPath
	 */
	public void removeCustomRoot(String rootPath) {
		customRoots.removeElement(rootPath);
	}

	/**
	 * Tests whether path starts with a custom file system root.
	 * 
	 * @param path
	 * @return <em>true</em> if path starts with an element of customRoots,
	 *         <em>false</em> otherwise
	 */
	private boolean startsWithCustomRoot(String path) {
		for (Enumeration<String> it = customRoots.elements(); it != null && it.hasMoreElements();) {
			if (path.startsWith(it.nextElement())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * returns the canonical form of path, if path doesn't start with one of
	 * '/';cwd;a customRoot; or is a WindowsRoot then prepend cwd to path
	 * 
	 * @param path
	 * @return canonical form of path
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	private String resolveRemotePath(String path) throws SftpStatusException, SshException {
		verifyConnection();

		String actual;
		if (!path.startsWith("/") && !path.startsWith(cwd) && !isWindowsRoot(path) && !startsWithCustomRoot(path)) {
			actual = cwd + (cwd.endsWith("/") ? "" : "/") + path;
		} else {
			actual = path;
		}

		if (!actual.equals("/") && actual.endsWith("/")) {
			return actual.substring(0, actual.length() - 1);
		} else {
			return actual;
		}
	}

	private void verifyConnection() throws SshException {
		if (sftp.isClosed()) {
			throw new SshException("The SFTP connection has been closed", SshException.REMOTE_HOST_DISCONNECTED);
		}
	}

	/**
	 * <p>
	 * Creates a new directory on the remote server. This method will throw an
	 * exception if the directory already exists. To create directories and
	 * disregard any errors use the <code>mkdirs</code> method.
	 * </p>
	 * 
	 * @param dir the name of the new directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void mkdir(String dir) throws SftpStatusException, SshException {
		String actual = resolveRemotePath(dir);

		if (Log.isDebugEnabled())
			Log.debug("Creating dir " + dir);

		SftpFileAttributes attrs = null;
		try {
			attrs = sftp.getAttributes(actual);
		} catch (SftpStatusException ex) {
			// only create the directory if catch an exception with code file
			// not found

			SftpFileAttributes newattrs = new SftpFileAttributes(SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY,
					sftp.getCharsetEncoding());
			;
			if (applyUmask) {
				newattrs.setPermissions(new UnsignedInteger32(0777 ^ umask));
			}
			sftp.makeDirectory(actual, newattrs);
			return;
		}

		if (Log.isDebugEnabled())
			Log.debug("File with name " + dir + " already exists!");

		throw new SftpStatusException(SftpStatusException.SSH_FX_FAILURE,
				(attrs.isDirectory() ? "Directory" : "File") + " already exists named " + dir);

	}

	/**
	 * <p>
	 * Create a directory or set of directories. This method will not fail even if
	 * the directories exist. It is advisable to test whether the directory exists
	 * before attempting an operation by using
	 * <a href="#stat(java.lang.String)">stat</a> to return the directories
	 * attributes.
	 * </p>
	 * 
	 * @param dir the path of directories to create.
	 */
	public void mkdirs(String dir) throws SftpStatusException, SshException {
		StringTokenizer tokens = new StringTokenizer(dir, "/");
		String path = dir.startsWith("/") ? "/" : "";

		while (tokens.hasMoreElements()) {
			path += (String) tokens.nextElement();

			try {
				stat(path);
			} catch (SftpStatusException ex) {
				try {
					mkdir(path);
				} catch (SftpStatusException ex2) {
					if (ex2.getStatus() == SftpStatusException.SSH_FX_PERMISSION_DENIED)
						throw ex2;
				}
			}

			path += "/";
		}
	}

	/**
	 * Determine whether the file object is pointing to a symbolic link that is
	 * pointing to a directory.
	 * 
	 * @return boolean
	 */
	public boolean isDirectoryOrLinkedDirectory(SftpFile file) throws SftpStatusException, SshException {
		return file.isDirectory() || (file.isLink() && stat(file.getAbsolutePath()).isDirectory());
	}

	/**
	 * <p>
	 * Returns the absolute path name of the current remote working directory.
	 * </p>
	 * 
	 * @return the absolute path of the remote working directory.
	 * @throws SshException
	 * @throws SftpStatusException
	 */
	public String pwd() throws SftpStatusException, SshException {
		return getAbsolutePath(cwd);
	}

	/**
	 * <p>
	 * List the contents of the current remote working directory.
	 * </p>
	 * 
	 * <p>
	 * Returns a list of <a href="../../maverick/ssh2/SftpFile.html">SftpFile</a>
	 * instances for the current working directory.
	 * </p>
	 * 
	 * @return a list of SftpFile for the current working directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * 
	 */
	public SftpFile[] ls() throws SftpStatusException, SshException {
		return ls(cwd);
	}

	/**
	 * <p>
	 * List the contents remote directory.
	 * </p>
	 * 
	 * <p>
	 * Returns a list of <a href="../../maverick/ssh2/SftpFile.html">SftpFile</a>
	 * instances for the remote directory.
	 * </p>
	 * 
	 * @param path the path on the remote server to list
	 * 
	 * @return a list of SftpFile for the remote directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFile[] ls(String path) throws SftpStatusException, SshException {

		String actual = resolveRemotePath(path);

		if (Log.isDebugEnabled())
			Log.debug("Listing files for " + actual);

		SftpFile file = sftp.openDirectory(actual);
		Vector<SftpFile> children = new Vector<SftpFile>();
		while (sftp.listChildren(file, children) > -1) {
			;
		}
		file.close();
		SftpFile[] files = new SftpFile[children.size()];
		int index = 0;
		for (Enumeration<SftpFile> e = children.elements(); e.hasMoreElements();) {
			files[index++] = e.nextElement();
		}
		return files;
	}

	public SftpFile[] ls(String filter, boolean regexFilter, int maximumFiles)
			throws SftpStatusException, SshException {
		return ls("", filter, regexFilter, maximumFiles);
	}

	public SftpFile[] ls(String path, String filter, boolean regexFilter, int maximumFiles)
			throws SftpStatusException, SshException {
		String actual = resolveRemotePath(path);

		if (Log.isDebugEnabled()) {
			Log.debug("Listing files for {} with filter {}");
		}

		ByteArrayWriter msg = new ByteArrayWriter();
		try {
			msg.writeString(actual);
			msg.writeString(filter);
			msg.writeBoolean(regexFilter);

			byte[] handle;
			boolean localFiltering = false;
			try {
				handle = sftp.getHandleResponse(
						sftp.sendExtensionMessage("open-directory-with-filter@sshtools.com", msg.toByteArray()));
			} catch (SftpStatusException e) {
				if (Boolean.getBoolean("maverick.disableLocalFiltering")) {
					throw new SshException("Remote server does not support server side filtering",
							SshException.UNSUPPORTED_OPERATION);
				}
				handle = sftp.openDirectory(actual).getHandle();
			}

			SftpFileFilter f = null;
			if (localFiltering) {
				f = regexFilter ? new RegexSftpFileFilter(filter) : new GlobSftpFileFilter(filter);
			}
			SftpFile file = new SftpFile(actual, sftp.getAttributes(actual));
			file.setHandle(handle);
			file.setSFTPSubsystem(sftp);

			Vector<SftpFile> children = new Vector<SftpFile>();
			Vector<SftpFile> tmp = new Vector<SftpFile>();

			int pageCount;
			do {
				pageCount = sftp.listChildren(file, tmp);

				if (pageCount > -1) {
					if (!localFiltering) {
						if (pageCount > -1 && Log.isDebugEnabled()) {
							Log.debug("Got page of {} files for {} with filter {}", pageCount, actual, filter,
									localFiltering);
						}
						children.addAll(tmp);
					} else {
						if (pageCount > -1 && Log.isDebugEnabled()) {
							Log.debug("Got page of {} files for {} before local filtering", pageCount, actual, filter,
									localFiltering);
						}
						int count = 0;
						for (SftpFile t : tmp) {
							if (f.matches(t.getFilename())) {
								children.add(t);
								count++;
							}
						}
						if (pageCount > -1 && Log.isDebugEnabled()) {
							Log.debug("Got page of {} files for {} after local filtering", count, actual, filter,
									localFiltering);
						}
					}
				}
			} while (pageCount > -1 && (maximumFiles == 0 || children.size() < maximumFiles));

			file.close();
			SftpFile[] files = new SftpFile[children.size()];
			int index = 0;
			for (Enumeration<SftpFile> e = children.elements(); e.hasMoreElements();) {
				files[index++] = e.nextElement();
			}
			return files;

		} catch (IOException e) {
			throw new SshException(SshException.INTERNAL_ERROR, e);
		} finally {
			try {
				msg.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Return an iterator for the current working directory.
	 * 
	 * This method improves memory usage by only getting paged contents of the
	 * directory.
	 * 
	 * @return
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public Iterator<SftpFile> lsIterator() throws SftpStatusException, SshException {
		return lsIterator(cwd);
	}

	/**
	 * Return an iterator for the path provided.
	 * 
	 * This method improves memory usage by only getting paged contents of the
	 * directory.
	 * 
	 * @param path
	 * @return
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public Iterator<SftpFile> lsIterator(String path) throws SftpStatusException, SshException {
		return new DirectoryIterator(path);
	}

	/**
	 * <p>
	 * Changes the local working directory.
	 * </p>
	 * 
	 * @param path the path to the new working directory
	 * 
	 * @throws SftpStatusException
	 * @throws IOException
	 * @throws PermissionDeniedException
	 */
	public void lcd(String path) throws SftpStatusException, IOException, PermissionDeniedException {
		AbstractFile actual = lcwd.resolveFile(path);

		if (!actual.isDirectory()) {
			throw new SftpStatusException(SftpStatusException.SSH_FX_FAILURE, path + " is not a directory");
		}

		lcwd = actual;

	}

	/**
	 * <p>
	 * Returns the absolute path to the local working directory.
	 * </p>
	 * 
	 * @return the absolute path of the local working directory.
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public String lpwd() throws IOException, PermissionDeniedException {
		return lcwd.getAbsolutePath();
	}
	
	public AbstractFile getCurrentWorkingDirectory() {
		return lcwd;
	}

	/**
	 * <p>
	 * Download the remote file to the local computer.
	 * </p>
	 * 
	 * @param path     the path to the remote file
	 * @param progress
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public SftpFileAttributes get(String path, FileTransferProgress progress) throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return get(path, progress, false);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer.
	 * </p>
	 * 
	 * @param path     the path to the remote file
	 * @param progress
	 * @param resume   attempt to resume a interrupted download
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public SftpFileAttributes get(String path, FileTransferProgress progress, boolean resume)
			throws SftpStatusException, SshException, TransferCancelledException, IOException,
			PermissionDeniedException {
		String localfile;

		if (path.lastIndexOf("/") > -1) {
			localfile = path.substring(path.lastIndexOf("/") + 1);
		} else {
			localfile = path;
		}

		return get(path, localfile, progress, resume);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer
	 * 
	 * @param path   the path to the remote file
	 * @param resume attempt to resume an interrupted download
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public SftpFileAttributes get(String path, boolean resume) throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return get(path, (FileTransferProgress) null, resume);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer
	 * 
	 * @param path the path to the remote file
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public SftpFileAttributes get(String path) throws SftpStatusException, SshException, TransferCancelledException,
			IOException, PermissionDeniedException {
		return get(path, (FileTransferProgress) null);
	}

	/**
	 * Get the target path of a symbolic link.
	 * 
	 * @param linkpath
	 * @return String
	 * @throws SshException if the remote SFTP version is < 3 an exception is thrown
	 *                      as this feature is not supported by previous versions of
	 *                      the protocol.
	 */
	public String getSymbolicLinkTarget(String linkpath) throws SftpStatusException, SshException {
		return sftp.getSymbolicLinkTarget(linkpath);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer. If the paths provided are not
	 * absolute the current working directory is used.
	 * </p>
	 * 
	 * @param remote   the path/name of the remote file
	 * @param local    the path/name to place the file on the local computer
	 * @param progress
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public SftpFileAttributes get(String remote, String local, FileTransferProgress progress)
			throws SftpStatusException, SshException, TransferCancelledException, IOException,
			PermissionDeniedException {
		return get(remote, local, progress, false);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer. If the paths provided are not
	 * absolute the current working directory is used.
	 * </p>
	 * 
	 * @param remote   the path/name of the remote file
	 * @param local    the path/name to place the file on the local computer
	 * @param progress
	 * @param resume   attempt to resume an interrupted download
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public SftpFileAttributes get(String remote, String local, FileTransferProgress progress, boolean resume)
			throws SftpStatusException, SshException, TransferCancelledException, IOException,
			PermissionDeniedException {

		// Moved here to ensure that stream is closed in finally
		OutputStream out = null;
		SftpFileAttributes attrs = null;

		// Perform local file operations first, then if it throws an exception
		// the server hasn't been unnecessarily loaded.
		AbstractFile localPath = resolveLocalPath(local);
		if (!localPath.exists()) {
			AbstractFile parent = localPath.resolveFile(FileUtils.getParentPath(localPath.getAbsolutePath()));
			parent.createFolder();
		}

		if (localPath.isDirectory()) {
			localPath = localPath.resolveFile(FileUtils.getFilename(remote));
		}

		// Check that file exists before we create a file
		stat(remote);

		long position = 0;

		try {

			// if resuming and the local file exists, then open as random access
			// file and seek to end of the file ready to continue writing
			if (resume && localPath.exists()) {
				out = localPath.getOutputStream(true);
				position = localPath.length();
			} else {
				out = localPath.getOutputStream();
			}

			attrs = get(remote, out, progress, position);

			return attrs;

		} catch (IOException ex) {
			throw new SftpStatusException(SftpStatusException.SSH_FX_FAILURE,
					"Failed to open outputstream to " + local);
		} finally {
			try {
				if (out != null)
					out.close();
				if (attrs != null) {
					localPath.setAttributes(attrs);
				}

			} catch (Throwable ex) {
				// NOTE: should we ignore this?
			}
		}
	}

	public String getRemoteNewline() throws SftpStatusException {
		return new String(sftp.getCanonicalNewline());
	}

	public int getRemoteEOL() throws SftpStatusException {
		return getEOL(sftp.getCanonicalNewline());
	}

	public int getEOL(String line) throws SftpStatusException {

		byte[] nl = line.getBytes();
		return getEOL(nl);
	}

	public int getEOL(byte[] nl) throws SftpStatusException {
		switch (nl.length) {
		case 1:
			if (nl[0] == '\r')
				return EOLProcessor.TEXT_CR;
			else if (nl[0] == '\n')
				return EOLProcessor.TEXT_LF;
			else
				throw new SftpStatusException(SftpStatusException.INVALID_HANDLE,
						"Unsupported text mode: invalid newline character");
		case 2:
			if (nl[0] == '\r' && nl[1] == '\n')
				return EOLProcessor.TEXT_CRLF;
			else
				throw new SftpStatusException(SftpStatusException.INVALID_HANDLE,
						"Unsupported text mode: invalid newline characters");
		default:
			throw new SftpStatusException(SftpStatusException.INVALID_HANDLE,
					"Unsupported text mode: newline length > 2");

		}
	}

	/**
	 * Download the remote file into the local file.
	 * 
	 * @param remote
	 * @param local
	 * @param resume attempt to resume an interrupted download
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public SftpFileAttributes get(String remote, String local, boolean resume) throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return get(remote, local, null, resume);
	}

	/**
	 * Download the remote file into the local file.
	 * 
	 * @param remote
	 * @param local
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public SftpFileAttributes get(String remote, String local) throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return get(remote, local, false);
	}

	/**
	 * <p>
	 * Download the remote file writing it to the specified
	 * <code>OutputStream</code>. The OutputStream is closed by this method even if
	 * the operation fails.
	 * </p>
	 * 
	 * @param remote   the path/name of the remote file
	 * @param local    the OutputStream to write
	 * @param progress
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public SftpFileAttributes get(String remote, OutputStream local, FileTransferProgress progress)
			throws SftpStatusException, SshException, TransferCancelledException {
		return get(remote, local, progress, 0);
	}

	/**
	 * constants for setting the regular expression syntax.
	 */
	public static final int NoSyntax = 0;
	public static final int GlobSyntax = 1;
	public static final int Perl5Syntax = 2;

	/**
	 * default regular expression syntax is to not perform regular expression
	 * matching on getFiles() and putFiles()
	 */
	private int RegExpSyntax = GlobSyntax;

	/**
	 * sets the type of regular expression matching to perform on gets and puts
	 * 
	 * @param syntax , NoSyntax for no regular expression matching, GlobSyntax for
	 *               GlobSyntax, Perl5Syntax for Perl5Syntax
	 */
	public void setRegularExpressionSyntax(int syntax) {
		RegExpSyntax = syntax;
	}

	/**
	 * Called by getFileMatches() to do regular expression pattern matching on the
	 * files in 'remote''s parent directory.
	 * 
	 * @param remote
	 * @return SftpFile[]
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFile[] matchRemoteFiles(String remote) throws SftpStatusException, SshException {

		String actualDir;
		String actualSearch;
		int fileSeparatorIndex;
		if ((fileSeparatorIndex = remote.lastIndexOf("/")) > -1) {
			actualDir = remote.substring(0, fileSeparatorIndex);
			actualSearch = remote.length() > fileSeparatorIndex + 1 ? remote.substring(fileSeparatorIndex + 1) : "";
		} else {
			actualDir = cwd;
			actualSearch = remote;
		}

		SftpFile[] files;

		RegularExpressionMatching matcher;
		switch (RegExpSyntax) {
		case GlobSyntax:
			matcher = new GlobRegExpMatching();
			files = ls(actualDir);
			break;
		case Perl5Syntax:
			matcher = new RegExpMatching();
			files = ls(actualDir);
			break;
		default:
			matcher = new NoRegExpMatching();
			files = new SftpFile[1];
			String actual = resolveRemotePath(remote);
			files[0] = getSubsystemChannel().getFile(actual);
		}

		return matcher.matchFilesWithPattern(files, actualSearch);
	}

	/**
	 * If RegExpSyntax is set to GlobSyntax or Perl5Syntax then it pattern matches
	 * the files in the remote directory using "remote" as a glob or perl5 Regular
	 * Expression. For each matching file get() is called to copy the file to the
	 * local directory.
	 * 
	 * <p>
	 * If RegExpSyntax is set to NoSyntax then "remote" is treated as a filepath
	 * instead of a regular expression
	 * </p>
	 * 
	 * @param remote
	 * @param progress
	 * @param streamOrFile
	 * @return SftpFile[] of SftpFile's that have been retrieved
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	private SftpFile[] getFileMatches(String remote, String local, FileTransferProgress progress, boolean resume)
			throws SftpStatusException, SshException, TransferCancelledException, IOException,
			PermissionDeniedException {

		// match with files using remote as regular expression.
		SftpFile[] matchedFiles = matchRemoteFiles(remote);

		Vector<SftpFile> retrievedFiles = new Vector<SftpFile>();
		// call get for each matched file, append the files attributes to a
		// vector to be returned at the end
		// call the correct get method depending on the get method that called
		// this
		for (int i = 0; i < matchedFiles.length; i++) {
			get(matchedFiles[i].getAbsolutePath(), local, progress, resume);
			retrievedFiles.addElement(matchedFiles[i]);
		}

		SftpFile[] retrievedSftpFiles = new SftpFile[retrievedFiles.size()];
		retrievedFiles.copyInto(retrievedSftpFiles);
		return retrievedSftpFiles;
	}

	/**
	 * Called by putFileMatches() to do regular expression pattern matching on the
	 * files in 'local''s parent directory.
	 * 
	 * @param local
	 * @return String[]
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	private String[] matchLocalFiles(String local)
			throws SftpStatusException, SshException, IOException, PermissionDeniedException {

		// Resolve the search path as it may not be CWD
		AbstractFile actualDir;
		String actualSearch;
		if (FileUtils.hasParents(local)) {
			actualDir = resolveLocalPath(FileUtils.getParentPath(local));
			actualSearch = FileUtils.getFilename(local);
		} else {
			actualDir = lcwd;
			actualSearch = local;
		}

		RegularExpressionMatching matcher;
		AbstractFile[] files;
		switch (RegExpSyntax) {
		case GlobSyntax:
			matcher = new GlobRegExpMatching();
			files = listFiles(actualDir);
			break;
		case Perl5Syntax:
			matcher = new RegExpMatching();
			files = listFiles(actualDir);
			break;
		default:
			matcher = new NoRegExpMatching();
			files = new AbstractFile[1];
			files[0] = lcwd.resolveFile(local);
		}

		return matcher.matchFileNamesWithPattern(files, actualSearch);
	}

	private AbstractFile[] listFiles(AbstractFile f) throws IOException, PermissionDeniedException {
		return f.getChildren().toArray(new AbstractFile[0]);
	}

	/**
	 * If RegExpSyntax is set to GlobSyntax or Perl5Syntax then it pattern matches
	 * the files in the local directory using "local" as a glob or perl5 Regular
	 * Expression. For each matching file put() is called to copy the file to the
	 * remote directory.
	 * 
	 * <p>
	 * If RegExpSyntax is set to NoSyntax then "local" is treated as a filepath
	 * instead of a regular expression.
	 * </p>
	 * 
	 * @param local
	 * @param progress
	 * @param streamOrFile
	 * 
	 * @throws IOException
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 */
	private void putFileMatches(String local, String remote, FileTransferProgress progress, boolean resume)
			throws IOException, SftpStatusException, SshException, TransferCancelledException,
			PermissionDeniedException {

		String remotePath = resolveRemotePath(remote);
		// Remote must be a valid remote directory

		SftpFileAttributes attrs = null;
		try {
			attrs = stat(remotePath);
		} catch (SftpStatusException ex) {
			throw new SftpStatusException(ex.getStatus(), "Remote path '" + remote
					+ "' does not exist. It must be a valid directory and must already exist!");
		}

		if (!attrs.isDirectory())
			throw new SftpStatusException(SftpStatusException.SSH_FX_NO_SUCH_PATH,
					"Remote path '" + remote + "' is not a directory!");

		String[] matchedFiles = matchLocalFiles(local);

		if (Log.isDebugEnabled()) {
			Log.debug("Matched {} files for {}", matchedFiles.length, local);
		}

		for (int i = 0; i < matchedFiles.length; i++) {
			try {
				put(matchedFiles[i], remotePath, progress, resume);
			} catch (SftpStatusException ex) {
				throw new SftpStatusException(ex.getStatus(),
						"Failed to put " + matchedFiles[i] + " to " + remote + " [" + ex.getMessage() + "]");
			}
		}
	}

	/**
	 * <p>
	 * Download the remote file writing it to the specified
	 * <code>OutputStream</code>. The OutputStream is closed by this method even if
	 * the operation fails.
	 * </p>
	 * 
	 * @param remote   the path/name of the remote file
	 * @param local    the OutputStream to write
	 * @param progress
	 * @param position the position within the file to start reading from
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public SftpFileAttributes get(String remote, OutputStream local, FileTransferProgress progress, long position)
			throws SftpStatusException, SshException, TransferCancelledException {

		String remotePath = resolveRemotePath(remote);
		SftpFileAttributes attrs = sftp.getAttributes(remotePath);

		if (position > attrs.getSize().longValue()) {
			throw new SftpStatusException(SftpStatusException.INVALID_RESUME_STATE,
					"The local file size is greater than the remote file");
		}

		if (progress != null) {
			progress.started(attrs.getSize().longValue() - position, remotePath);
		}

		SftpFile file;

		if (transferMode == MODE_TEXT && sftp.getVersion() > 3) {
			file = sftp.openFile(remotePath, SftpChannel.OPEN_READ | SftpChannel.OPEN_TEXT);

		} else {
			file = sftp.openFile(remotePath, SftpChannel.OPEN_READ);

		}

		try {

			if (transferMode == MODE_TEXT) {

				// Default text mode handling for versions 3- of the SFTP
				// protocol
				int inputStyle = outputEOL;
				int outputStyle = (stripEOL ? EOLProcessor.TEXT_ALL : inputEOL);

				byte[] nl = null;

				if (sftp.getVersion() <= 3 && sftp.getExtension("newline@vandyke.com") != null) {
					nl = sftp.getExtension("newline@vandyke.com");
				} else if (sftp.getVersion() > 3) {
					nl = sftp.getCanonicalNewline();
				}

				// Setup text mode correctly if were using version 4+ of the
				// SFTP protocol
				if (nl != null && !forceRemoteEOL) {
					inputStyle = getEOL(new String(nl));

				}

				local = EOLProcessor.createOutputStream(inputStyle, outputStyle, local);
			}

			sftp.performOptimizedRead(remotePath, file.getHandle(), attrs.getSize().longValue(), blocksize, local,
					asyncRequests, progress, position);
		} catch (IOException ex) {
			throw new SftpStatusException(SftpStatusException.SSH_FX_FAILURE,
					"Failed to open text conversion outputstream");
		} catch (TransferCancelledException tce) {
			throw tce;
		} finally {

			try {
				local.close();
			} catch (Throwable t) {
			}
			try {
				sftp.closeFile(file);
			} catch (SftpStatusException ex) {
			}
		}

		if (progress != null) {
			progress.completed();
		}

		return attrs;
	}

	/**
	 * Create an InputStream for reading a remote file.
	 * 
	 * @param remotefile
	 * @param position
	 * @return InputStream
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public InputStream getInputStream(String remotefile, long position) throws SftpStatusException, SshException {
		String remotePath = resolveRemotePath(remotefile);
		sftp.getAttributes(remotePath);

		return new SftpFileInputStream(sftp.openFile(remotePath, SftpChannel.OPEN_READ), position);

	}

	/**
	 * Create an InputStream for reading a remote file.
	 * 
	 * @param remotefile
	 * @return InputStream
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public InputStream getInputStream(String remotefile) throws SftpStatusException, SshException {
		return getInputStream(remotefile, 0);
	}

	/**
	 * Download the remote file into an OutputStream.
	 * 
	 * @param remote
	 * @param local
	 * @param position the position from which to start reading the remote file
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public SftpFileAttributes get(String remote, OutputStream local, long position)
			throws SftpStatusException, SshException, TransferCancelledException {
		return get(remote, local, null, position);
	}

	/**
	 * Download the remote file into an OutputStream.
	 * 
	 * @param remote
	 * @param local
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public SftpFileAttributes get(String remote, OutputStream local)
			throws SftpStatusException, SshException, TransferCancelledException {
		return get(remote, local, null, 0);
	}

	/**
	 * <p>
	 * Returns the state of the SFTP client. The client is closed if the underlying
	 * session channel is closed. Invoking the <code>quit</code> method of this
	 * object will close the underlying session channel.
	 * </p>
	 * 
	 * @return true if the client is still connected, otherwise false
	 */
	public boolean isClosed() {
		return sftp.isClosed();
	}

	/**
	 * <p>
	 * Upload a file to the remote computer.
	 * </p>
	 * 
	 * @param local    the path/name of the local file
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public void put(String local, FileTransferProgress progress, boolean resume) throws SftpStatusException,
			SshException, TransferCancelledException, IOException, PermissionDeniedException {
		AbstractFile f = resolveLocalPath(local);
		put(local, f.getName(), progress, resume);
	}

	/**
	 * <p>
	 * Upload a file to the remote computer.
	 * </p>
	 * 
	 * @param local    the path/name of the local file
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public void put(String local, FileTransferProgress progress) throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		put(local, progress, false);
	}

	/**
	 * Upload a file to the remote computer
	 * 
	 * @param local
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public void put(String local) throws SftpStatusException, SshException, TransferCancelledException, IOException,
			PermissionDeniedException {
		put(local, false);
	}

	/**
	 * Upload a file to the remote computer
	 * 
	 * @param local
	 * @param resume attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public void put(String local, boolean resume) throws SftpStatusException, SshException, TransferCancelledException,
			IOException, PermissionDeniedException {
		put(local, (FileTransferProgress) null, resume);
	}

	/**
	 * <p>
	 * Upload a file to the remote computer. If the paths provided are not absolute
	 * the current working directory is used.
	 * </p>
	 * 
	 * @param local    the path/name of the local file
	 * @param remote   the path/name of the destination file
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public void put(String local, String remote, FileTransferProgress progress) throws SftpStatusException,
			SshException, TransferCancelledException, IOException, PermissionDeniedException {
		put(local, remote, progress, false);
	}

	public void append(InputStream in, String remote)
			throws SftpStatusException, SshException, TransferCancelledException {
		put(in, remote, null, -1, -1);
	}

	@Deprecated
	public void append(InputStream in, String remote, FileTransferProgress progress)
			throws SftpStatusException, SshException, TransferCancelledException {
		append(in, remote, progress, -1);
	}

	public void append(InputStream in, String remote, FileTransferProgress progress, long length)
			throws SftpStatusException, SshException, TransferCancelledException {
		put(in, remote, progress, -1, length);
	}

	/**
	 * <p>
	 * Upload a file to the remote computer. If the paths provided are not absolute
	 * the current working directory is used.
	 * </p>
	 * 
	 * @param local    the path/name of the local file
	 * @param remote   the path/name of the destination file
	 * @param progress
	 * @param resume   attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public void put(String local, String remote, FileTransferProgress progress, boolean resume)
			throws SftpStatusException, SshException, TransferCancelledException, IOException,
			PermissionDeniedException {
		AbstractFile localPath = resolveLocalPath(local);

		InputStream in = localPath.getInputStream();
		// File f = new File(local);
		long position = 0;
		long length = localPath.length();

		SftpFileAttributes attrs = null;

		try {
			attrs = stat(remote);
			if (attrs.isDirectory()) {
				remote += (remote.endsWith("/") ? "" : "/") + localPath.getName();

				attrs = stat(remote);
			}

		} catch (SftpStatusException ex) {
			resume = false;
		}

		if (resume) {
			if (localPath.length() <= attrs.getSize().longValue()) {
				try {
					in.close();
				} catch (IOException e) {
				}
				throw new SftpStatusException(SftpStatusException.INVALID_RESUME_STATE,
						"The remote file size is greater than the local file");
			}
			try {
				position = attrs.getSize().longValue();
				in.skip(position);
			} catch (IOException ex) {
				try {
					in.close();
				} catch (IOException e) {
				}
				throw new SftpStatusException(SftpStatusException.SSH_FX_NO_SUCH_FILE, ex.getMessage());
			}

		}

		try {
			put(in, remote, progress, position, length);

		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}

	}

	public void append(String local, String remote) throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		append(local, remote, null);
	}

	@Deprecated
	public void append(String local, String remote, FileTransferProgress progress) throws SftpStatusException,
			SshException, TransferCancelledException, IOException, PermissionDeniedException {
		append(local, remote, progress, - 1);
	}

	public void append(String local, String remote, FileTransferProgress progress, long length) throws SftpStatusException,
			SshException, TransferCancelledException, IOException, PermissionDeniedException {
		AbstractFile localPath = resolveLocalPath(local);

		String remotePath = resolveRemotePath(remote);
		stat(remotePath);

		InputStream in = localPath.getInputStream();

		try {
			append(in, remotePath, progress, length);

		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Upload a file to the remote computer
	 * 
	 * @param local
	 * @param remote
	 * @param resume attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public void put(String local, String remote, boolean resume) throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		put(local, remote, null, resume);
	}

	/**
	 * Upload a file to the remote computer
	 * 
	 * @param local
	 * @param remote
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public void put(String local, String remote) throws SftpStatusException, SshException, TransferCancelledException,
			IOException, PermissionDeniedException {
		put(local, remote, null, false);
	}

	/**
	 * <p>
	 * Upload a file to the remote computer reading from the specified <code>
	 * InputStream</code>. The InputStream is closed, even if the operation fails.
	 * The {@link FileTransferProgress} will be indeterminate, i.e {@link FileTransferProgress#started(long, String)} will
	 * be called with a <code>length</code> of <code>-1</code>, as it is not possible to generically determine the length of
	 * an {@link InputStream}. It is recommended you use {@link #put(InputStream, String, FileTransferProgress, long, long)} instead
	 * to provide the size when known. 
	 * </p>
	 * 
	 * @param in       the InputStream being read
	 * @param remote   the path/name of the destination file
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public void put(InputStream in, String remote, FileTransferProgress progress)
			throws SftpStatusException, SshException, TransferCancelledException {
		put(in, remote, progress, 0, -1);
	}

	@Deprecated
	public void put(InputStream in, String remote, FileTransferProgress progress, long position)
			throws SftpStatusException, SshException, TransferCancelledException {
		put(in, remote, progress, position, -1);
	}

	/**
	 * <p>
	 * Upload a file to the remote computer reading from the specified <code>
	 * InputStream</code>. The InputStream is closed, even if the operation fails.
	 * The {@link FileTransferProgress} will be indeterminate if you pass a <code>length</code> of <code>-1</code>.
	 * to provide the size when known. 
	 * </p>
	 * 
	 * @param in       the InputStream being read
	 * @param remote   the path/name of the destination file
	 * @param progress progress
	 * @param position position to start at
	 * @param length   the number of bytes that will be transferred or -1 if unknown.
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public void put(InputStream in, String remote, FileTransferProgress progress, long position, long length)
			throws SftpStatusException, SshException, TransferCancelledException {
		String remotePath = resolveRemotePath(remote);

		SftpFileAttributes attrs = null;

		if (transferMode == MODE_TEXT) {

			// Default text mode handling for versions 3- of the SFTP protocol
			int inputStyle = (stripEOL ? EOLProcessor.TEXT_ALL : inputEOL);
			int outputStyle = outputEOL;

			byte[] nl = null;

			if (sftp.getVersion() <= 3 && sftp.getExtension("newline@vandyke.com") != null) {
				nl = sftp.getExtension("newline@vandyke.com");
			} else if (sftp.getVersion() > 3) {
				nl = sftp.getCanonicalNewline();
			}
			// Setup text mode correctly if were using version 4+ of the
			// SFTP protocol
			if (nl != null & !forceRemoteEOL) {
				outputStyle = getEOL(nl);
			}

			try {
				in = EOLProcessor.createInputStream(inputStyle, outputStyle, in);
			} catch (IOException ex) {
				throw new SshException("Failed to create EOL processing stream", SshException.INTERNAL_ERROR);
			}
		}

		attrs = new SftpFileAttributes(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR, "UTF-8");

		if (applyUmask) {
			attrs.setPermissions(new UnsignedInteger32(0666 ^ umask));
		}

		if (position > 0) {

			if (transferMode == MODE_TEXT && sftp.getVersion() > 3) {
				throw new SftpStatusException(SftpStatusException.SSH_FX_OP_UNSUPPORTED,
						"Resume on text mode files is not supported");
			}

			internalPut(length, in, remotePath, progress, position, SftpChannel.OPEN_WRITE, attrs);
		} else {

			if (position == 0) {
				if (transferMode == MODE_TEXT && sftp.getVersion() > 3) {
					internalPut(length, in, remotePath, progress, position, SftpChannel.OPEN_CREATE | SftpChannel.OPEN_TRUNCATE
							| SftpChannel.OPEN_WRITE | SftpChannel.OPEN_TEXT, attrs);
				} else {
					internalPut(length, in, remotePath, progress, position,
							SftpChannel.OPEN_CREATE | SftpChannel.OPEN_TRUNCATE | SftpChannel.OPEN_WRITE, attrs);
				}
			} else {
				/**
				 * Negative position means append
				 */
				if (transferMode == MODE_TEXT && sftp.getVersion() > 3) {
					internalPut(length, in, remotePath, progress, position,
							SftpChannel.OPEN_WRITE | SftpChannel.OPEN_TEXT | SftpChannel.OPEN_APPEND, attrs);
				} else {
					internalPut(length, in, remotePath, progress, position, SftpChannel.OPEN_WRITE | SftpChannel.OPEN_APPEND,
							attrs);
				}
			}
		}
	}

	private void internalPut(long length, InputStream in, String remotePath, FileTransferProgress progress, long position, int flags,
			SftpFileAttributes attrs) throws SftpStatusException, SshException, TransferCancelledException {

		SftpFile file = sftp.openFile(remotePath, flags, attrs);
		if (progress != null) {
			progress.started(length, remotePath);
		}

		try {
			sftp.performOptimizedWrite(remotePath, file.getHandle(), blocksize, asyncRequests, in, buffersize, progress,
					position < 0 ? 0 : position);
		} catch (SftpStatusException e) {
			Log.error("SFTP status exception during transfer [" + e.getStatus() + "]", e);
			throw e;
		} catch (SshException e) {
			Log.error("SSH exception during transfer [" + e.getReason() + "]", e);
			if (e.getCause() != null) {
				Log.error("SSH exception cause", e.getCause());
			}
			throw e;
		} catch (TransferCancelledException e) {
			Log.error("Transfer cancelled", e);
			throw e;
		} finally {
			try {
				in.close();
			} catch (Throwable t) {
			}
			sftp.closeFile(file);
		}

		if (progress != null) {
			progress.completed();
		}
	}

	/**
	 * Create an OutputStream for writing to a remote file.
	 * 
	 * @param remotefile
	 * @return OutputStream
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public OutputStream getOutputStream(String remotefile) throws SftpStatusException, SshException {

		String remotePath = resolveRemotePath(remotefile);
		return new SftpFileOutputStream(sftp.openFile(remotePath,
				SftpChannel.OPEN_CREATE | SftpChannel.OPEN_TRUNCATE | SftpChannel.OPEN_WRITE));

	}

	/**
	 * Upload the contents of an InputStream to the remote computer.
	 * 
	 * @param in
	 * @param remote
	 * @param position
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public void put(InputStream in, String remote, long position)
			throws SftpStatusException, SshException, TransferCancelledException {
		put(in, remote, null, position, -1);
	}

	/**
	 * Upload the contents of an InputStream to the remote computer.
	 * 
	 * @param in
	 * @param remote
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public void put(InputStream in, String remote)
			throws SftpStatusException, SshException, TransferCancelledException {
		put(in, remote, null, 0, -1);
	}

	/**
	 * <p>
	 * Sets the user ID to owner for the file or directory.
	 * </p>
	 * 
	 * @param uid  numeric user id of the new owner
	 * @param path the path to the remote file/directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * 
	 */
	public void chown(String uid, String path) throws SftpStatusException, SshException {
		String actual = resolveRemotePath(path);

		SftpFileAttributes attrs = sftp.getAttributes(actual);
		SftpFileAttributes newAttrs = new SftpFileAttributes(attrs.getType(), sftp.getCharsetEncoding());
		newAttrs.setUID(uid);
		if (sftp.getVersion() <= 3) {
			newAttrs.setGID(attrs.getGID());
		}
		sftp.setAttributes(actual, newAttrs);

	}

	/**
	 * <p>
	 * Sets the user ID to owner for the file or directory.
	 * </p>
	 * 
	 * @param uid  numeric user id of the new owner
	 * @param path the path to the remote file/directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * 
	 */
	public void chown(String uid, String gid, String path) throws SftpStatusException, SshException {
		String actual = resolveRemotePath(path);

		SftpFileAttributes attrs = sftp.getAttributes(actual);
		SftpFileAttributes newAttrs = new SftpFileAttributes(attrs.getType(), sftp.getCharsetEncoding());
		newAttrs.setUID(uid);
		newAttrs.setGID(gid);
		sftp.setAttributes(actual, newAttrs);

	}
	
	/**
	 * <p>
	 * Sets the group ID for the file or directory.
	 * </p>
	 * 
	 * @param gid  the numeric group id for the new group
	 * @param path the path to the remote file/directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void chgrp(String gid, String path) throws SftpStatusException, SshException {
		String actual = resolveRemotePath(path);

		SftpFileAttributes attrs = sftp.getAttributes(actual);
		SftpFileAttributes newAttrs = new SftpFileAttributes(attrs.getType(), sftp.getCharsetEncoding());
		newAttrs.setGID(gid);
		if (sftp.getVersion() <= 3) {
			newAttrs.setUID(attrs.getUID());
		}
		sftp.setAttributes(actual, newAttrs);

	}

	/**
	 * <p>
	 * Changes the access permissions or modes of the specified file or directory.
	 * </p>
	 * 
	 * <p>
	 * Modes determine who can read, change or execute a file.
	 * </p>
	 * <blockquote>
	 * 
	 * <pre>
	 * Absolute modes are octal numbers specifying the complete list of
	 * attributes for the files; you specify attributes by OR'ing together
	 * these bits.
	 * 
	 * 0400       Individual read
	 * 0200       Individual write
	 * 0100       Individual execute (or list directory)
	 * 0040       Group read
	 * 0020       Group write
	 * 0010       Group execute
	 * 0004       Other read
	 * 0002       Other write
	 * 0001       Other execute
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @param permissions the absolute mode of the file/directory
	 * @param path        the path to the file/directory on the remote server
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void chmod(int permissions, String path) throws SftpStatusException, SshException {
		String actual = resolveRemotePath(path);
		sftp.changePermissions(actual, permissions);
	}

	/**
	 * Sets the umask for this client.<br>
	 * <blockquote>
	 * 
	 * <pre>
	 * To give yourself full permissions for both files and directories and
	 * prevent the group and other users from having access:
	 * 
	 *   umask(&quot;077&quot;);
	 * 
	 * This subtracts 077 from the system defaults for files and directories
	 * 666 and 777. Giving a default access permissions for your files of
	 * 600 (rw-------) and for directories of 700 (rwx------).
	 * 
	 * To give all access permissions to the group and allow other users read
	 * and execute permission:
	 * 
	 *   umask(&quot;002&quot;);
	 * 
	 * This subtracts 002 from the system defaults to give a default access permission
	 * for your files of 664 (rw-rw-r--) and for your directories of 775 (rwxrwxr-x).
	 * 
	 * To give the group and other users all access except write access:
	 * 
	 *   umask(&quot;022&quot;);
	 * 
	 * This subtracts 022 from the system defaults to give a default access permission
	 * for your files of 644 (rw-r--r--) and for your directories of 755 (rwxr-xr-x).
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @param umask
	 * @throws SshException
	 */
	public void umask(String umask) throws SshException {
		try {
			this.umask = Integer.parseInt(umask, 8);
			applyUmask = true;
		} catch (NumberFormatException ex) {
			throw new SshException("umask must be 4 digit octal number e.g. 0022", SshException.BAD_API_USAGE);
		}
	}

	/**
	 * Rename a file on the remote computer, optionally using posix semantics that
	 * allow files to be renamed even if the destination path exists. The server
	 * must support posix-rename@openssh.com SFTP extension in order to use the
	 * posix operation.
	 * 
	 * @param oldpath
	 * @param newpath
	 * @param posix
	 * @throws IOException
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void rename(String oldpath, String newpath, boolean posix)
			throws IOException, SftpStatusException, SshException {

		if (posix) {
			ByteArrayWriter msg = new ByteArrayWriter();

			try {
				msg.writeString(resolveRemotePath(oldpath));
				msg.writeString(resolveRemotePath(newpath));

				sftp.getOKRequestStatus(sftp.sendExtensionMessage("posix-rename@openssh.com", msg.toByteArray()));

			} finally {
				msg.close();
			}
		} else {
			rename(oldpath, newpath);
		}
	}

	public void copyRemoteFile(String sourceFile, String destinationFile, boolean overwriteDestination)
			throws SftpStatusException, SshException, IOException {

		ByteArrayWriter msg = new ByteArrayWriter();

		try {
			msg.writeString(resolveRemotePath(sourceFile));
			msg.writeString(resolveRemotePath(destinationFile));
			msg.writeBoolean(overwriteDestination);

			sftp.getOKRequestStatus(sftp.sendExtensionMessage("copy-file", msg.toByteArray()));

		} finally {
			msg.close();
		}
	}

	public void copyRemoteData(SftpFile sourceFile, UnsignedInteger64 fromOffset, UnsignedInteger64 length,
			SftpFile destinationFile, UnsignedInteger64 toOffset)
			throws SftpStatusException, SshException, IOException {

		if (!sourceFile.isOpen() || !destinationFile.isOpen()) {
			throw new SftpStatusException(SftpStatusException.SSH_FX_INVALID_HANDLE,
					"source and desintation files must be open");
		}

		try (ByteArrayWriter msg = new ByteArrayWriter()) {
			msg.writeBinaryString(sourceFile.getHandle());
			msg.writeUINT64(fromOffset);
			msg.writeUINT64(length);
			msg.writeBinaryString(destinationFile.getHandle());
			msg.writeUINT64(toOffset);

			sftp.getOKRequestStatus(sftp.sendExtensionMessage("copy-data", msg.toByteArray()));

		}
	}

	/**
	 * <p>
	 * Rename a file on the remote computer.
	 * </p>
	 * 
	 * @param oldpath the old path
	 * @param newpath the new path
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void rename(String oldpath, String newpath) throws SftpStatusException, SshException {
		String from = resolveRemotePath(oldpath);
		String to = resolveRemotePath(newpath);

		SftpFileAttributes attrs = null;

		try {
			attrs = sftp.getAttributes(to);

		} catch (SftpStatusException ex) {
			sftp.renameFile(from, to);
			return;
		}

		if (attrs != null && attrs.isDirectory()) {
			sftp.renameFile(from, FileUtils.checkEndsWithSlash(to) + FileUtils.lastPathElement(from));
		} else {
			throw new SftpStatusException(SftpStatusException.SSH_FX_FILE_ALREADY_EXISTS,
					newpath + " already exists on the remote filesystem");
		}

	}

	/**
	 * <p>
	 * Remove a file or directory from the remote computer.
	 * </p>
	 * 
	 * @param path the path of the remote file/directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void rm(String path) throws SftpStatusException, SshException {
		String actual = resolveRemotePath(path);

		SftpFileAttributes attrs = sftp.getAttributes(actual);
		if (attrs.isDirectory()) {
			sftp.removeDirectory(actual);
		} else {
			sftp.removeFile(actual);
		}
	}

	/**
	 * Remove a file or directory on the remote computer with options to force
	 * deletion of existing files and recursion.
	 * 
	 * @param path
	 * @param force
	 * @param recurse
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void rm(String path, boolean force, boolean recurse) throws SftpStatusException, SshException {
		String actual = resolveRemotePath(path);

		SftpFileAttributes attrs = null;

		attrs = sftp.getAttributes(actual);

		SftpFile file;

		if (attrs.isDirectory()) {
			SftpFile[] list = ls(path);

			if (!force && (list.length > 0)) {
				throw new SftpStatusException(SftpStatusException.SSH_FX_FAILURE,
						"You cannot delete non-empty directory, use force=true to overide");
			}
			for (int i = 0; i < list.length; i++) {
				file = list[i];

				if (file.isDirectory() && !file.getFilename().equals(".") && !file.getFilename().equals("..")) {
					if (recurse) {
						rm(file.getAbsolutePath(), force, recurse);
					} else {
						throw new SftpStatusException(SftpStatusException.SSH_FX_FAILURE,
								"Directory has contents, cannot delete without recurse=true");
					}
				} else if (file.isFile() || file.isLink()) {
					sftp.removeFile(file.getAbsolutePath());
				}
			}

			sftp.removeDirectory(actual);
		} else {
			sftp.removeFile(actual);
		}
	}

	/**
	 * <p>
	 * Create a symbolic link on the remote computer.
	 * </p>
	 * 
	 * @param path the path to the existing file
	 * @param link the new link
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void symlink(String path, String link) throws SftpStatusException, SshException {
		String actualPath = resolveRemotePath(path);
		String actualLink = resolveRemotePath(link);

		sftp.createSymbolicLink(actualLink, actualPath);
	}

	/**
	 * <p>
	 * Returns the attributes of the file from the remote computer.
	 * </p>
	 * 
	 * @param path the path of the file on the remote computer
	 * 
	 * @return the attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFileAttributes stat(String path) throws SftpStatusException, SshException {
		String actual = resolveRemotePath(path);
		return sftp.getAttributes(actual);
	}

	/**
	 * <p>
	 * Returns the attributes of the link from the remote computer.
	 * </p>
	 * 
	 * @param path the path of the file on the remote computer
	 * 
	 * @return the attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFileAttributes statLink(String path) throws SftpStatusException, SshException {
		String actual = resolveRemotePath(path);
		return sftp.getLinkAttributes(actual);
	}

	/**
	 * Get the absolute path for a file.
	 * 
	 * @param path
	 * 
	 * @return String
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public String getAbsolutePath(String path) throws SftpStatusException, SshException {
		String actual = resolveRemotePath(path);
		return sftp.getAbsolutePath(actual);
	}

	/**
	 * Verify a local and remote file. Requires a minimum SFTP version of 5 and/or
	 * support of the "md5-hash" extension
	 * 
	 * @param localFile
	 * @param remoteFile
	 * @return
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws IOException
	 * @throws PermissionDeniedException
	 */
	public boolean verifyFiles(String localFile, String remoteFile)
			throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		return verifyFiles(localFile, remoteFile, 0, 0);
	}

	public boolean verifyFiles(String localFile, String remoteFile, RemoteHash algorithm)
			throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		return verifyFiles(localFile, remoteFile, 0, 0, algorithm);
	}

	/**
	 * Verify a local and remote file. Requires a minimum SFTP version of 5 and/or
	 * support of the "md5-hash" extension.
	 * 
	 * @param localFile
	 * @param remoteFile
	 * @param offset
	 * @param length
	 * @return
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws IOException
	 * @throws PermissionDeniedException
	 */
	public boolean verifyFiles(String localFile, String remoteFile, long offset, long length)
			throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		return verifyFiles(localFile, remoteFile, offset, length, RemoteHash.md5);
	}

	public boolean verifyFiles(String localFile, String remoteFile, long offset, long length, RemoteHash algorithm)
			throws SftpStatusException, SshException, IOException, PermissionDeniedException {

		AbstractFile local = resolveLocalPath(localFile);
		if (!local.exists()) {
			throw new IOException("Local file " + localFile + " does not exist!");
		}

		try {
			MessageDigest md = null;
			switch (algorithm) {
			case md5:
				md = MessageDigest.getInstance(JCEAlgorithms.JCE_MD5);
				break;
			case sha1:
				md = MessageDigest.getInstance(JCEAlgorithms.JCE_SHA1);
				break;
			case sha256:
				md = MessageDigest.getInstance(JCEAlgorithms.JCE_SHA256);
				break;
			case sha512:
				md = MessageDigest.getInstance(JCEAlgorithms.JCE_SHA512);
				break;
			}

			byte[] remoteHash = getRemoteHash(remoteFile, offset, length, algorithm);

			if (Log.isDebugEnabled()) {
				Log.debug("Remote hash for {} is {}", remoteFile, Utils.bytesToHex(remoteHash));
			}

			try(var dis = new DigestInputStream(local.getInputStream(), md)) {
				if (offset > 0) {
					dis.skip(offset);
				}

				if (length > 0) {
					IOUtils.copy(dis, OutputStream.nullOutputStream(), length);
				} else {
					IOUtils.copy(dis, OutputStream.nullOutputStream());
				}
			} 

			byte[] localHash = md.digest();

			if (Log.isDebugEnabled()) {
				Log.debug("Local hash for {} is {}", localFile, Utils.bytesToHex(localHash));
			}

			return Arrays.equals(remoteHash, localHash);

		} catch (NoSuchAlgorithmException e1) {
			throw new SshException(SshException.INTERNAL_ERROR, e1);
		} catch (IOException e1) {
			throw new SshException(SshException.INTERNAL_ERROR, e1);
		}
	}

	@Deprecated
	public byte[] getRemoteHash(String remoteFile) throws IOException, SftpStatusException, SshException {
		return getRemoteHash(remoteFile, 0, 0, new byte[0]);
	}

	@Deprecated
	public byte[] getRemoteHash(String remoteFile, long offset, long length, byte[] quickCheck)
			throws IOException, SftpStatusException, SshException {
		ByteArrayWriter msg = new ByteArrayWriter();

		try {
			msg.writeString(resolveRemotePath(remoteFile));
			msg.writeUINT64(offset);
			msg.writeUINT64(length);
			msg.writeBinaryString(quickCheck);

			SftpMessage resp = sftp.getExtensionResponse(sftp.sendExtensionMessage("md5-hash", msg.toByteArray()));

			resp.readString();
			return resp.readBinaryString();
		} finally {
			msg.close();
		}

	}

	@Deprecated
	public byte[] getRemoteHash(byte[] handle) throws IOException, SftpStatusException, SshException {
		return getRemoteHash(handle, 0, 0, new byte[0]);
	}

	@Deprecated
	public byte[] getRemoteHash(byte[] handle, long offset, long length, byte[] quickCheck)
			throws IOException, SftpStatusException, SshException {

		return doMD5HashHandle(handle, offset, length, quickCheck);
	}

	public byte[] getRemoteHash(byte[] handle, RemoteHash algorithm)
			throws IOException, SftpStatusException, SshException {
		return getRemoteHash(handle, 0, 0, algorithm);
	}

	public byte[] getRemoteHash(byte[] handle, long offset, long length, RemoteHash algorithm)
			throws IOException, SftpStatusException, SshException {

		return doCheckHashHandle(handle, offset, length, algorithm);
	}

	public byte[] getRemoteHash(String path, RemoteHash algorithm)
			throws IOException, SftpStatusException, SshException {
		return getRemoteHash(path, 0, 0, algorithm);
	}

	public byte[] getRemoteHash(String path, long offset, long length, RemoteHash algorithm)
			throws IOException, SftpStatusException, SshException {

		String actual = resolveRemotePath(path);
		return doCheckFileHandle(actual, offset, length, algorithm);

	}

	protected byte[] doCheckHashHandle(byte[] handle, long offset, long length, RemoteHash algorithm)
			throws IOException, SftpStatusException, SshException {

		ByteArrayWriter msg = new ByteArrayWriter();

		try {
			msg.writeBinaryString(handle);
			msg.writeString(algorithm.name());
			msg.writeUINT64(offset);
			msg.writeUINT64(length);
			msg.writeInt(0L);

			return processCheckFileResponse(
					sftp.getExtensionResponse(sftp.sendExtensionMessage("check-file-handle", msg.toByteArray())),
					algorithm);

		} finally {
			msg.close();
		}
	}

	protected byte[] doCheckFileHandle(String filename, long offset, long length, RemoteHash algorithm)
			throws IOException, SftpStatusException, SshException {

		ByteArrayWriter msg = new ByteArrayWriter();

		try {
			msg.writeString(filename);
			msg.writeString(algorithm.name());
			msg.writeUINT64(offset);
			msg.writeUINT64(length);
			msg.writeInt(0L);

			return processCheckFileResponse(
					sftp.getExtensionResponse(sftp.sendExtensionMessage("check-file-name", msg.toByteArray())),
					algorithm);

		} finally {
			msg.close();
		}
	}

	protected byte[] processCheckFileResponse(SftpMessage resp, RemoteHash algorithm) throws IOException {

		String processedAlgorithm = resp.readString();
		if (!processedAlgorithm.equals(algorithm.name())) {
			throw new IOException("Remote server returned a hash in an unsupported algorithm");
		}

		int hashLength;
		switch (algorithm) {
		case md5:
			hashLength = 16;
			break;
		case sha1:
			hashLength = 20;
			break;
		case sha256:
			hashLength = 32;
			break;
		case sha512:
			hashLength = 64;
			break;
		default:
			throw new IOException("Unsupported hash algorihm " + processedAlgorithm);
		}
		byte[] hash = new byte[hashLength];
		if (resp.available() < hash.length) {
			throw new IOException("Unexpected hash length returned by remote server");
		}

		resp.readFully(hash);
		return hash;

	}

	protected byte[] doMD5HashHandle(byte[] handle, long offset, long length, byte[] quickCheck)
			throws IOException, SftpStatusException, SshException {

		ByteArrayWriter msg = new ByteArrayWriter();

		try {
			msg.writeBinaryString(handle);
			msg.writeUINT64(offset);
			msg.writeUINT64(length);
			msg.writeBinaryString(quickCheck);

			SftpMessage resp = sftp
					.getExtensionResponse(sftp.sendExtensionMessage("md5-hash-handle", msg.toByteArray()));

			resp.readString();
			return resp.readBinaryString();
		} finally {
			msg.close();
		}

	}

	/**
	 * <p>
	 * Close the SFTP client.
	 * </p>
	 * 
	 */
	public void quit() throws SshException {
		sftp.close();
	}

	/**
	 * <p>
	 * Close the SFTP client.
	 * </p>
	 * 
	 */
	public void exit() throws SshException {
		sftp.close();
	}

	/**
	 * Copy the contents of a local directory into a remote directory.
	 * 
	 * @param localdir  the path to the local directory
	 * @param remotedir the remote directory which will receive the contents
	 * @param recurse   recurse through child folders
	 * @param sync      synchronize the directories by removing files on the remote
	 *                  server that do not exist locally
	 * @param commit    actually perform the operation. If <tt>false</tt> a
	 *                  <a href="DirectoryOperation.html">DirectoryOperation</a>
	 *                  will be returned so that the operation can be evaluated and
	 *                  no actual files will be created/transfered.
	 * @param progress
	 * 
	 * @return DirectoryOperation
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 */
	public DirectoryOperation putLocalDirectory(String localdir, String remotedir, boolean recurse, boolean sync,
			boolean commit, FileTransferProgress progress) throws IOException, SftpStatusException, SshException,
			TransferCancelledException, PermissionDeniedException {
		DirectoryOperation op = new DirectoryOperation();

		AbstractFile local = resolveLocalPath(localdir);

		remotedir = resolveRemotePath(remotedir);
		remotedir += (remotedir.endsWith("/") ? "" : "/");

		// Setup the remote directory if were committing
		if (commit) {
			try {
				sftp.getAttributes(remotedir);
			} catch (SftpStatusException ex) {
				mkdirs(remotedir);
			}
		}

		// List the local files and verify against the remote server
		AbstractFile[] sources = listFiles(local);

		for (AbstractFile source : sources) {

			if (source.isDirectory() && !source.getName().equals(".") && !source.getName().equals("..")) {
				if (recurse) {
					// File f = new File(local, source.getName());
					op.addDirectoryOperation(putLocalDirectory(source.getAbsolutePath(), remotedir + source.getName(),
							recurse, sync, commit, progress), source);
				}
			} else if (source.isFile()) {

				boolean newFile = false;
				boolean unchangedFile = false;

				try {
					SftpFileAttributes attrs = sftp.getAttributes(remotedir + source.getName());
					unchangedFile = ((source.length() == attrs.getSize().longValue())
							&& ((source.lastModified() / 1000) == attrs.getModifiedTime().longValue()));

					System.out.println(source.getName() + " is " + (unchangedFile ? "unchanged" : "changed"));

				} catch (SftpStatusException ex) {
					System.out.println(source.getName() + " is new");
					newFile = true;
				}

				try {

					if (commit && !unchangedFile) { // BPS - Added
						// !unChangedFile test.
						// Why would want to
						// copy that has been
						// determined to be
						// unchanged?
						put(source.getAbsolutePath(), remotedir + source.getName(), progress);
						SftpFileAttributes attrs = sftp.getAttributes(remotedir + source.getName());
						attrs.setTimes(new UnsignedInteger64(source.lastModified() / 1000),
								new UnsignedInteger64(source.lastModified() / 1000));
						sftp.setAttributes(remotedir + source.getName(), attrs);
					}

					if (unchangedFile) {
						op.addUnchangedFile(source);
					} else if (!newFile) {
						op.addUpdatedFile(source);
					} else {
						op.addNewFile(source);
					}

				} catch (SftpStatusException ex) {
					op.addFailedTransfer(source, ex);
				}
			}
		}

		if (sync) {
			// List the contents of the new remote directory and remove any
			// files/directories that were not updated
			try {
				SftpFile[] files = ls(remotedir);
				SftpFile file;

				AbstractFile f;

				for (int i = 0; i < files.length; i++) {
					file = files[i];

					// Create a local file object to test for its existence
					f = local.resolveFile(file.getFilename());

					if (!op.containsFile(f) && !file.getFilename().equals(".") && !file.getFilename().equals("..")) {
						op.addDeletedFile(file);

						if (commit) {
							if (file.isDirectory()) {
								// Recurse through the directory, deleting stuff
								recurseMarkForDeletion(file, op);

								if (commit) {
									rm(file.getAbsolutePath(), true, true);
								}
							} else if (file.isFile()) {
								rm(file.getAbsolutePath());
							}
						}
					}
				}
			} catch (SftpStatusException ex2) {
				// Ignore since if it does not exist we cant delete it
			}
		}

		// Return the operation details
		return op;
	}

	private String[] getChildNames(AbstractFile local) throws IOException, PermissionDeniedException {
		List<String> children = new ArrayList<>();
		for (AbstractFile child : local.getChildren()) {
			children.add(child.getName());
		}
		return children.toArray(new String[0]);
	}

	private void recurseMarkForDeletion(SftpFile file, DirectoryOperation op) throws SftpStatusException, SshException {
		SftpFile[] list = ls(file.getAbsolutePath());
		op.addDeletedFile(file);

		for (int i = 0; i < list.length; i++) {
			file = list[i];

			if (file.isDirectory() && !file.getFilename().equals(".") && !file.getFilename().equals("..")) {
				recurseMarkForDeletion(file, op);
			} else if (file.isFile()) {
				op.addDeletedFile(file);
			}
		}
	}

	private void recurseMarkForDeletion(AbstractFile file, DirectoryOperation op)
			throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		String[] list = getChildNames(file);
		op.addDeletedFile(file);

		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				file = file.resolveFile(list[i]);

				if (file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..")) {
					recurseMarkForDeletion(file, op);
				} else if (file.isFile()) {
					op.addDeletedFile(file);
				}
			}
		}
	}

	/**
	 * Format a String with the details of the file. <blockquote>
	 * 
	 * <pre>
	 * -rwxr-xr-x   1 mjos     staff      348911 Mar 25 14:29 t-filexfer
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @param file
	 * @throws SftpStatusException
	 * @throws SshException
	 * @return String
	 */
	public static String formatLongname(SftpFile file) throws SftpStatusException, SshException {
		return formatLongname(file.getAttributes(), file.getFilename());
	}

	/**
	 * Format a String with the details of the file. <blockquote>
	 * 
	 * <pre>
	 * -rwxr-xr-x   1 mjos     staff      348911 Mar 25 14:29 t-filexfer
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @param attrs
	 * @param filename
	 * @return String
	 */
	public static String formatLongname(SftpFileAttributes attrs, String filename) {

		StringBuffer str = new StringBuffer();
		str.append(pad(10 - attrs.getPermissionsString().length()) + attrs.getPermissionsString());
		str.append("    1 ");
		str.append(attrs.getUID() + pad(8 - attrs.getUID().length())); // uid
		str.append(" ");
		str.append(attrs.getGID() + pad(8 - attrs.getGID().length())); // gid
		str.append(" ");
		str.append(pad(8 - attrs.getSize().toString().length()) + attrs.getSize().toString());
		str.append(" ");
		str.append(pad(12 - getModTimeString(attrs.getModifiedTime()).length())
				+ getModTimeString(attrs.getModifiedTime()));
		str.append(" ");
		str.append(filename);

		return str.toString();
	}

	private static String getModTimeString(UnsignedInteger64 mtime) {
		if (mtime == null) {
			return "";
		}

		SimpleDateFormat df;
		long mt = (mtime.longValue() * 1000L);
		long now = System.currentTimeMillis();

		if ((now - mt) > (6 * 30 * 24 * 60 * 60 * 1000L)) {
			df = new SimpleDateFormat("MMM dd  yyyy");
		} else {
			df = new SimpleDateFormat("MMM dd hh:mm");
		}

		return df.format(new Date(mt));
	}

	private static String pad(int num) {

		StringBuffer strBuf = new StringBuffer("");
		if (num > 0) {
			for (int i = 0; i < num; i++) {
				strBuf.append(" ");
			}
		}

		return strBuf.toString();
	}

	/**
	 * Copy the contents of a remote directory to a local directory
	 * 
	 * @param remotedir the remote directory whose contents will be copied.
	 * @param localdir  the local directory to where the contents will be copied
	 * @param recurse   recurse into child folders
	 * @param sync      synchronized the directories by removing files and
	 *                  directories that do not exist on the remote server.
	 * @param commit    actually perform the operation. If <tt>false</tt> the
	 *                  operation will be processed and a
	 *                  <a href="DirectoryOperation.html">DirectoryOperation</a>
	 *                  will be returned without actually transfering any files.
	 * @param progress
	 * 
	 * @return DirectoryOperation
	 * 
	 * @throws IOException
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 */
	public DirectoryOperation getRemoteDirectory(String remotedir, String localdir, boolean recurse, boolean sync,
			boolean commit, FileTransferProgress progress) throws IOException, SftpStatusException, SshException,
			TransferCancelledException, PermissionDeniedException {
		// Create an operation object to hold the information
		DirectoryOperation op = new DirectoryOperation();

		// Record the previous working directoies
		String pwd = pwd();
		// String lpwd = lpwd();
		cd(remotedir);

		// Setup the local cwd
		String base = remotedir;

		if (base.endsWith("/"))
			base = base.substring(0, base.length() - 1);

		int idx = base.lastIndexOf('/');

		if (idx != -1) {
			base = base.substring(idx + 1);
		}

		AbstractFile local = resolveLocalPath(localdir);

		if (!local.exists() && commit) {
			local.createFolder();
		}

		SftpFile[] files = ls();
		SftpFile file;
		AbstractFile f;

		for (int i = 0; i < files.length; i++) {
			file = files[i];
			System.out.println("Process: " + file.getAbsolutePath());

			if (file.isDirectory() && !file.getFilename().equals(".") && !file.getFilename().equals("..")) {
				if (recurse) {
					System.out.println("   is dir " + file.getAbsolutePath());
					f = local.resolveFile(file.getFilename());
					op.addDirectoryOperation(getRemoteDirectory(file.getFilename(),
							local.getAbsolutePath() + "/" + file.getFilename(), recurse, sync, commit, progress), f);
				}
			} else if (file.isFile()) {
				f = local.resolveFile(file.getFilename());

				System.out.println("   file " + f.getAbsolutePath() + " (exists " + f.exists() + ") len: " + f.length()
						+ " vs " + file.getAttributes().getSize().longValue() + " date: " + (f.lastModified() / 1000)
						+ " vs " + file.getAttributes().getModifiedTime().longValue());

				if (f.exists() && (f.length() == file.getAttributes().getSize().longValue())
						&& ((f.lastModified() / 1000) == file.getAttributes().getModifiedTime().longValue())) {

					System.out.println("   is unchanged " + file.getAbsolutePath());
					if (commit) {
						op.addUnchangedFile(f);
					} else {
						op.addUnchangedFile(file);
					}

					continue;
				}

				try {

					if (f.exists()) {
						System.out.println("   is updated " + file.getAbsolutePath());
						if (commit) {
							op.addUpdatedFile(f);
						} else {
							op.addUpdatedFile(file);
						}
					} else {
						System.out.println("   is new " + file.getAbsolutePath());
						if (commit) {
							op.addNewFile(f);
						} else {
							op.addNewFile(file);
						}
					}

					if (commit) {
						// Get the file
						System.out.println("   get " + file.getAbsolutePath());
						get(file.getFilename(), f.getAbsolutePath(), progress);
					}

				} catch (SftpStatusException ex) {
					op.addFailedTransfer(f, ex);
				}
			}
		}

		if (sync) {
			// List the contents of the new local directory and remove any
			// files/directories that were not updated
			String[] contents = getChildNames(local);
			AbstractFile f2;
			if (contents != null) {
				for (int i = 0; i < contents.length; i++) {
					f2 = local.resolveFile(contents[i]);
					if (!op.containsFile(f2)) {
						op.addDeletedFile(f2);

						if (f2.isDirectory() && !f2.getName().equals(".") && !f2.getName().equals("..")) {
							System.out.println("   delete recurse into " + f2.getAbsolutePath());
							recurseMarkForDeletion(f2, op);

							if (commit) {
								f2.delete(true);
							}
						} else if (commit) {
							System.out.println("   delete " + f2.getAbsolutePath());
							f2.delete(false);
						}
					}
				}
			}
		}

		cd(pwd);

		return op;
	}

	/**
	 * <p>
	 * Download the remote files to the local computer
	 * </p>
	 * 
	 * <p>
	 * When RegExpSyntax is set to NoSyntax the getFiles() methods act identically
	 * to the get() methods except for a different return type.
	 * </p>
	 * 
	 * <p>
	 * When RegExpSyntax is set to GlobSyntax or Perl5Syntax, getFiles() treats
	 * 'remote' as a regular expression, and gets all the files in 'remote''s parent
	 * directory that match the pattern. The default parent directory of remote is
	 * the remote cwd unless 'remote' contains file seperators(/).
	 * </p>
	 * 
	 * <p>
	 * Examples can be found in SftpConnect.java
	 * 
	 * <p>
	 * Code Example: <blockquote>
	 * 
	 * <pre>
	 * // change reg exp syntax from default SftpClient.NoSyntax (no reg exp matching)
	 * // to SftpClient.GlobSyntax
	 * sftp.setRegularExpressionSyntax(SftpClient.GlobSyntax);
	 * // get all .doc files with 'rfc' in their names, in the 'docs/unsorted/' folder
	 * // relative to the remote cwd, and copy them to the local cwd.
	 * sftp.getFiles(&quot;docs/unsorted/*rfc*.doc&quot;);
	 * </pre>
	 * 
	 * </blockquote>
	 * </p>
	 * 
	 * @param remote the regular expression path to the remote file
	 * 
	 * @return the downloaded files' attributes
	 * 
	 * @throws IOException
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 */
	public SftpFile[] getFiles(String remote) throws IOException, SftpStatusException, SshException,
			TransferCancelledException, PermissionDeniedException {
		return getFiles(remote, (FileTransferProgress) null);
	}

	/**
	 * <p>
	 * Download the remote files to the local computer
	 * 
	 * @param remote the regular expression path to the remote file
	 * @param resume attempt to resume an interrupted download
	 * 
	 * @return the downloaded files' attributes
	 * 
	 * @throws IOException
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 */
	public SftpFile[] getFiles(String remote, boolean resume) throws IOException, SftpStatusException, SshException,
			TransferCancelledException, PermissionDeniedException {
		return getFiles(remote, (FileTransferProgress) null, resume);
	}

	/**
	 * <p>
	 * Download the remote files to the local computer.
	 * </p>
	 * 
	 * @param remote   the regular expression path to the remote file
	 * @param progress
	 * 
	 * @return SftpFile[]
	 * 
	 * @throws IOException
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 */
	public SftpFile[] getFiles(String remote, FileTransferProgress progress) throws IOException, SftpStatusException,
			SshException, TransferCancelledException, PermissionDeniedException {
		return getFiles(remote, progress, false);
	}

	/**
	 * <p>
	 * Download the remote files to the local computer.
	 * </p>
	 * 
	 * @param remote   the regular expression path to the remote file
	 * @param progress
	 * @param resume   attempt to resume a interrupted download
	 * 
	 * @return SftpFile[]
	 * 
	 * @throws IOException
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 */
	public SftpFile[] getFiles(String remote, FileTransferProgress progress, boolean resume) throws IOException,
			SftpStatusException, SshException, TransferCancelledException, PermissionDeniedException {
		return getFiles(remote, lcwd.getAbsolutePath(), progress, resume);
	}

	/**
	 * Download the remote files into the local file.
	 * 
	 * @param remote
	 * @param local
	 * 
	 * @return SftpFile[]
	 * 
	 * @throws IOException
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 */
	public SftpFile[] getFiles(String remote, String local) throws IOException, SftpStatusException, SshException,
			TransferCancelledException, PermissionDeniedException {
		return getFiles(remote, local, false);
	}

	/**
	 * Download the remote files into the local file.
	 * 
	 * @param remote
	 * @param local
	 * @param resume attempt to resume an interrupted download
	 * 
	 * @return SftpFile[]
	 * 
	 * @throws IOException
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 */
	public SftpFile[] getFiles(String remote, String local, boolean resume) throws IOException, SftpStatusException,
			SshException, TransferCancelledException, PermissionDeniedException {
		return getFiles(remote, local, null, resume);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer. If the paths provided are not
	 * absolute the current working directory is used.
	 * </p>
	 * 
	 * @param remote   the regular expression path/name of the remote files
	 * @param local    the path/name to place the file on the local computer
	 * @param progress
	 * 
	 * @return SftpFile[]
	 * 
	 * @throws SftpStatusException
	 * @throws IOException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException
	 */
	public SftpFile[] getFiles(String remote, String local, FileTransferProgress progress, boolean resume)
			throws IOException, SftpStatusException, SshException, TransferCancelledException,
			PermissionDeniedException {
		return getFileMatches(remote, local, progress, resume);
	}

	/**
	 * <p>
	 * Upload the contents of an InputStream to the remote computer.
	 * </p>
	 * 
	 * <p>
	 * When RegExpSyntax is set to NoSyntax the putFiles() methods act identically
	 * to the put() methods except for a different return type.
	 * </p>
	 * 
	 * <p>
	 * When RegExpSyntax is set to GlobSyntax or Perl5Syntax, putFiles() treats
	 * 'local' as a regular expression, and gets all the files in 'local''s parent
	 * directory that match the pattern. The default parent directory of local is
	 * the local cwd unless 'local' contains file seperators.
	 * </p>
	 * 
	 * <p>
	 * Examples can be found in SftpConnect.java
	 * 
	 * <p>
	 * Code Example: <blockquote>
	 * 
	 * <pre>
	 * // change reg exp syntax from default SftpClient.NoSyntax (no reg exp matching)
	 * // to SftpClient.GlobSyntax
	 * sftp.setRegularExpressionSyntax(SftpClient.GlobSyntax);
	 * // put all .doc files with 'rfc' in their names, in the 'docs/unsorted/' folder
	 * // relative to the local cwd, and copy them to the remote cwd.
	 * sftp.putFiles(&quot;docs/unsorted/*rfc*.doc&quot;);
	 * </pre>
	 * 
	 * </blockquote>
	 * </p>
	 * 
	 * @param local
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws IOException
	 * @throws PermissionDeniedException
	 */
	public void putFiles(String local) throws IOException, SftpStatusException, SshException,
			TransferCancelledException, PermissionDeniedException {
		putFiles(local, false);
	}

	/**
	 * Upload files to the remote computer
	 * 
	 * @param local
	 * @param resume attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws IOException
	 * @throws PermissionDeniedException
	 */
	public void putFiles(String local, boolean resume) throws IOException, SftpStatusException, SshException,
			TransferCancelledException, PermissionDeniedException {
		putFiles(local, (FileTransferProgress) null, resume);
	}

	/**
	 * <p>
	 * Upload files to the remote computer
	 * </p>
	 * 
	 * @param local    the regular expression path/name of the local files
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws IOException
	 * @throws PermissionDeniedException
	 */
	public void putFiles(String local, FileTransferProgress progress) throws IOException, SftpStatusException,
			SshException, TransferCancelledException, PermissionDeniedException {
		putFiles(local, progress, false);
	}

	/**
	 * <p>
	 * Upload files to the remote computer
	 * </p>
	 * 
	 * @param local    the regular expression path/name of the local files
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws IOException
	 * @throws PermissionDeniedException
	 */
	public void putFiles(String local, FileTransferProgress progress, boolean resume) throws IOException,
			SftpStatusException, SshException, TransferCancelledException, PermissionDeniedException {
		putFiles(local, pwd(), progress, resume);
	}

	/**
	 * Upload files to the remote computer
	 * 
	 * @param local
	 * @param remote
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws IOException
	 * @throws PermissionDeniedException
	 */
	public void putFiles(String local, String remote) throws IOException, SftpStatusException, SshException,
			TransferCancelledException, PermissionDeniedException {
		putFiles(local, remote, null, false);
	}

	/**
	 * Upload files to the remote computer
	 * 
	 * @param local
	 * @param remote
	 * @param resume attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws IOException
	 * @throws PermissionDeniedException
	 */
	public void putFiles(String local, String remote, boolean resume) throws IOException, SftpStatusException,
			SshException, TransferCancelledException, PermissionDeniedException {
		putFiles(local, remote, null, resume);
	}

	/**
	 * <p>
	 * Upload files to the remote computer. If the paths provided are not absolute
	 * the current working directory is used.
	 * </p>
	 * 
	 * @param local    the regular expression path/name of the local files
	 * @param remote   the path/name of the destination file
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws IOException
	 * @throws PermissionDeniedException
	 */
	public void putFiles(String local, String remote, FileTransferProgress progress) throws IOException,
			SftpStatusException, SshException, TransferCancelledException, PermissionDeniedException {
		putFiles(local, remote, progress, false);
	}

	/**
	 * make local copies of some of the variables, then call putfilematches, which
	 * calls "put" on each file that matches the regexp local.
	 * 
	 * @param local    the regular expression path/name of the local files
	 * @param remote   the path/name of the destination file
	 * @param progress
	 * @param resume   attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws IOException
	 * @throws PermissionDeniedException
	 */
	public void putFiles(String local, String remote, FileTransferProgress progress, boolean resume) throws IOException,
			SftpStatusException, SshException, TransferCancelledException, PermissionDeniedException {
		putFileMatches(local, remote, progress, resume);
	}

	/**
	 * A simple wrapper class to provide an OutputStream to a RandomAccessFile
	 * 
	 * 
	 */
	static class RandomAccessFileOutputStream extends OutputStream {

		RandomAccessFile file;

		RandomAccessFileOutputStream(RandomAccessFile file) {
			this.file = file;
		}

		public void write(int b) throws IOException {
			file.write(b);
		}

		public void write(byte[] buf, int off, int len) throws IOException {
			file.write(buf, off, len);
		}

		public void close() throws IOException {
			file.close();
		}
	}

	class DirectoryIterator implements Iterator<SftpFile> {

		SftpFile currentFolder;
		Vector<SftpFile> currentPage = new Vector<SftpFile>();
		Iterator<SftpFile> currentIterator;

		DirectoryIterator(String path) throws SftpStatusException, SshException {

			String actual = resolveRemotePath(path);

			if (Log.isDebugEnabled())
				Log.debug("Listing files for " + actual);

			currentFolder = sftp.openDirectory(actual);

			try {
				getNextPage();
			} catch (EOFException e) {
			}

		}

		private void getNextPage() throws SftpStatusException, SshException, EOFException {
			currentPage.clear();
			int ret = sftp.listChildren(currentFolder, currentPage);
			if (ret == -1) {
				currentIterator = null;
				throw new EOFException();
			}
			currentIterator = currentPage.iterator();
		}

		@Override
		public boolean hasNext() {
			if (currentIterator != null && currentIterator.hasNext()) {
				return true;
			}
			return false;
		}

		@Override
		public SftpFile next() {
			if (currentIterator == null) {
				throw new NoSuchElementException();
			}

			SftpFile ret = null;
			if (currentIterator.hasNext()) {
				ret = currentIterator.next();
			}

			if (!currentIterator.hasNext()) {
				try {
					getNextPage();
				} catch (EOFException e) {
					if (ret == null) {
						throw new NoSuchElementException();
					}
				} catch (SftpStatusException | SshException e) {
					throw new NoSuchElementException(e.getMessage());
				}

				if (ret == null) {
					ret = currentIterator.next();
				}
			}

			return ret;
		}

	}

	public boolean isConnected() {
		return sftp.isClosed();
	}

	public void hardlink(String src, String dst) throws SshException, SftpStatusException {

		try (ByteArrayWriter msg = new ByteArrayWriter()) {
			msg.writeString(src);
			msg.writeString(dst);
			SftpChannel channel = getSubsystemChannel();
			UnsignedInteger32 requestId = channel.sendExtensionMessage("hardlink@openssh.com", msg.toByteArray());
			channel.getOKRequestStatus(requestId);
		} catch (IOException e) {
			throw new SshException(e);
		}
	}

	public String getHomeDirectory(String username) throws SshException, SftpStatusException {

		try (ByteArrayWriter msg = new ByteArrayWriter()) {
			msg.writeString(username);
			SftpChannel channel = getSubsystemChannel();
			UnsignedInteger32 requestId = channel.sendExtensionMessage("home-directory", msg.toByteArray());
			return channel.getSingleFileResponse(channel.getResponse(requestId), "SSH_FXP_NAME").getAbsolutePath();
		} catch (IOException e) {
			throw new SshException(e);
		}

	}

	public String makeTemporaryFolder() throws SshException, SftpStatusException {

		SftpChannel channel = getSubsystemChannel();
		UnsignedInteger32 requestId = channel.sendExtensionMessage("make-temp-folder", null);
		return channel.getSingleFileResponse(channel.getResponse(requestId), "SSH_FXP_NAME").getAbsolutePath();

	}

	public String getTemporaryFolder() throws SshException, SftpStatusException {

		SftpChannel channel = getSubsystemChannel();
		UnsignedInteger32 requestId = channel.sendExtensionMessage("get-temp-folder", null);
		return channel.getSingleFileResponse(channel.getResponse(requestId), "SSH_FXP_NAME").getAbsolutePath();
	}

	public StatVfs statVFS(String path) throws SshException, SftpStatusException {

		try (ByteArrayWriter msg = new ByteArrayWriter()) {
			msg.writeString(path);
			SftpChannel channel = getSubsystemChannel();
			UnsignedInteger32 requestId = channel.sendExtensionMessage("statvfs@openssh.com", msg.toByteArray());
			return new StatVfs(channel.getResponse(requestId));
		} catch (IOException e) {
			throw new SshException(e);
		}
	}

	public String getHome() throws SftpStatusException, SshException {
		return getAbsolutePath("");
	}

	@Override
	public void close() throws IOException {
		try {
			this.quit();
		} catch (SshException e) {
			throw new SshIOException(e);
		}
	}

	public FileVisitResult visit(String path, FileVisitor<SftpFile> visitor) throws SshException, SftpStatusException {
		SftpFileAttributes attrs = stat(path);
		SftpFile file = new SftpFile(path, attrs);
		try {
			if (attrs.isDirectory()) {
				FileVisitResult preVisitResult = visitor.preVisitDirectory(file, fileToBasicAttributes(file));
				try {
					if (preVisitResult != FileVisitResult.CONTINUE)
						return preVisitResult;

					for (SftpFile child : ls(path)) {
						if (child.isLink() || child.isFile()) {
							FileVisitResult fileVisitResult = visitor.visitFile(child, fileToBasicAttributes(child));
							if (fileVisitResult != FileVisitResult.CONTINUE
									&& fileVisitResult != FileVisitResult.SKIP_SUBTREE)
								return fileVisitResult;
						} else if (child.isDirectory() && !child.getFilename().equals(".")
								&& !child.getFilename().equals("..")) {
							switch (visit(child.getAbsolutePath(), visitor)) {
							case SKIP_SIBLINGS:
								break;
							case TERMINATE:
								return FileVisitResult.TERMINATE;
							default:
								continue;
							}
						}
					}

					FileVisitResult postVisitResult = visitor.postVisitDirectory(file, null);
					if (postVisitResult != FileVisitResult.CONTINUE && postVisitResult != FileVisitResult.SKIP_SUBTREE)
						return postVisitResult;
				} catch (SftpStatusException ioe) {
					FileVisitResult postVisitResult = visitor.postVisitDirectory(file, new IOException(ioe));
					if (postVisitResult != FileVisitResult.CONTINUE && postVisitResult != FileVisitResult.SKIP_SUBTREE)
						return postVisitResult;
				}
			} else {
				FileVisitResult fileVisitResult = visitor.visitFile(file, fileToBasicAttributes(file));
				if (fileVisitResult != FileVisitResult.CONTINUE && fileVisitResult != FileVisitResult.SKIP_SUBTREE)
					return fileVisitResult;
			}
		} catch (IOException ioe) {
			throw new SshException(ioe);
		}
		return FileVisitResult.CONTINUE;
	}

	private BasicFileAttributes fileToBasicAttributes(SftpFile file) {
		SftpFileAttributes attrs = checkAttributes(file);
		return new BasicFileAttributes() {
			@Override
			public FileTime creationTime() {
				return FileTime.fromMillis(attrs.getCreationDateTime().getTime());
			}

			@Override
			public Object fileKey() {
				return attrs;
			}

			@Override
			public boolean isDirectory() {
				return attrs.isDirectory();
			}

			@Override
			public boolean isOther() {
				return !attrs.isDirectory() && !attrs.isFile() && !attrs.isLink();
			}

			@Override
			public boolean isRegularFile() {
				return attrs.isFile();
			}

			@Override
			public boolean isSymbolicLink() {
				return attrs.isLink();
			}

			@Override
			public FileTime lastAccessTime() {
				return FileTime.fromMillis(attrs.getAccessedDateTime().getTime());
			}

			@Override
			public FileTime lastModifiedTime() {
				return FileTime.fromMillis(attrs.getModifiedDateTime().getTime());
			}

			@Override
			public long size() {
				return attrs.getSize().longValue();
			}
		};
	}

	private SftpFileAttributes checkAttributes(SftpFile file) {
		try {
			return file.getAttributes();
		} catch (SftpStatusException | SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}
