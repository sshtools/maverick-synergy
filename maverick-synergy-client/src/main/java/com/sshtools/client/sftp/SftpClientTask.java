
package com.sshtools.client.sftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.sshtools.client.SshClient;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.client.tasks.Task;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;

/**
 * An abstract task that implements an SFTP client.
 */
public abstract class SftpClientTask extends Task {

	SftpClient sftp;
	
	public SftpClientTask(SshConnection con) {
		super(con);
	}
	
	public SftpClientTask(SshClient ssh) {
		super(ssh.getConnection());
	}
	
	protected void doTask() {
		
		try {
			sftp = new SftpClient(con);
			
			SftpClientTask.this.doSftp();

			done(true);
			
			sftp.exit();
		} catch (SshException | PermissionDeniedException | IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	
	protected abstract void doSftp();
	
	
	/**
	 * Sets the block size used when transferring files, defaults to the
	 * optimized setting of 32768. You should not increase this value as the
	 * remote server may not be able to support higher blocksizes.
	 * 
	 * @param blocksize
	 */
	public void setBlockSize(int blocksize) {
		sftp.setBlockSize(blocksize);
	}

	/**
	 * Returns the instance of the AbstractSftpChannel used by this class
	 * 
	 * @return the AbstractSftpChannel instance
	 */
	public SftpChannel getSubsystemChannel() {
		return sftp.getSubsystemChannel();
	}

	/**
	 * <p>
	 * Sets the transfer mode for current operations. The valid modes are:<br>
	 * <br>
	 * {@link #MODE_BINARY} - Files are transferred in binary mode and no
	 * processing of text files is performed (default mode).<br>
	 * <br>
	 * {@link #MODE_TEXT} - For servers supporting version 4+ of the SFTP
	 * protocol files are transferred in text mode. For earlier protocol
	 * versions the files are transfered in binary mode but the client performs
	 * processing of text; if files are written to the remote server the client
	 * ensures that the line endings conform to the remote EOL mode set using
	 * {@link setRemoteEOL(int)}. For files retrieved from the server the EOL
	 * policy is based upon System policy as defined by the "line.seperator"
	 * system property.
	 * </p>
	 * 
	 * @param transferMode
	 *            int
	 */
	public void setTransferMode(int transferMode) {
		sftp.setTransferMode(transferMode);
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
	 * Valid values for this method are {@link EOL_CRLF} (default),
	 * {@link EOL_CR}, and {@link EOL_LF}.
	 * </p>
	 * 
	 * @param eolMode
	 *            int
	 */
	public void setRemoteEOL(int eolMode) {
		sftp.setRemoteEOL(eolMode);
	}

	/**
	 * <p>
	 * Override the default local system EOL for text mode files.
	 * </p>
	 * 
	 * <p>
	 * Valid values for this method are {@link EOL_CRLF} (default),
	 * {@link EOL_CR}, and {@link EOL_LF}.
	 * </p>
	 * 
	 * @param eolMode
	 *            int
	 */
	public void setLocalEOL(int eolMode) {
		sftp.setLocalEOL(eolMode);

	}
	
	/**
	 * Override automatic detection of the remote EOL (any SFTP version). USE WITH CAUTION.
	 * @param forceRemoteEOL
	 */
	public void setForceRemoteEOL(boolean forceRemoteEOL) {
		sftp.setForceRemoteEOL(forceRemoteEOL);
	}
	
	/**
	 * 
	 * @return int
	 */
	public int getTransferMode() {
		return sftp.getTransferMode();
	}

	/**
	 * Set the size of the buffer which is used to read from the local file
	 * system. This setting is used to optimize the writing of files by allowing
	 * for a large chunk of data to be read in one operation from a local file.
	 * The previous version simply read each block of data before sending
	 * however this decreased performance, this version now reads the file into
	 * a temporary buffer in order to reduce the number of local filesystem
	 * reads. This increases performance and so this setting should be set to
	 * the highest value possible. The default setting is negative which means
	 * the entire file will be read into a temporary buffer.
	 * 
	 * @param buffersize
	 */
	public void setBufferSize(int buffersize) {
		sftp.setBufferSize(buffersize);
	}

	/**
	 * Set the maximum number of asynchronous requests that are outstanding at
	 * any one time. This setting is used to optimize the reading and writing of
	 * files to/from the remote file system when using the get and put methods.
	 * The default for this setting is 100.
	 * 
	 * @param asyncRequests
	 */
	public void setMaxAsyncRequests(int asyncRequests) {
		sftp.setMaxAsyncRequests(asyncRequests);
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
		return sftp.umask(umask);
	}

	public SftpFile openFile(String fileName) throws SftpStatusException,
		SshException {
		return openFile(fileName, SftpChannel.OPEN_READ);
	}

	public SftpFile openFile(String fileName, int flags) throws SftpStatusException,
			SshException {
		return sftp.openFile(fileName, flags);
	}

	public SftpFile openDirectory(String path) throws SftpStatusException, SshException {
		return sftp.openDirectory(path);
	}

	public List<SftpFile> readDirectory(SftpFile dir) throws SftpStatusException, SshException {
		return sftp.readDirectory(dir);
	}
	/**
	 * <p>
	 * Changes the working directory on the remote server, or the user's default
	 * directory if <code>null</code> or any empty string is provided as the
	 * directory path. The user's default directory is typically their home
	 * directory but is dependent upon server implementation.
	 * </p>
	 * 
	 * @param dir
	 *            the new working directory
	 * 
	 * @throws IOException
	 *             if an IO error occurs or the file does not exist
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void cd(String dir) throws SftpStatusException, SshException {
		sftp.cd(dir);
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
	public String getDefaultDirectory() throws SftpStatusException,
			SshException {
		return sftp.getDefaultDirectory();
	}

	/**
	 * Change the working directory to the parent directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void cdup() throws SftpStatusException, SshException {
		sftp.cdup();
	}


	/**
	 * Add a custom file system root path such as "flash:"
	 * 
	 * @param rootPath
	 */
	public void addCustomRoot(String rootPath) {
		sftp.addCustomRoot(rootPath);
	}

	/**
	 * Remove a custom file system root path such as "flash:"
	 * 
	 * @param rootPath
	 */
	public void removeCustomRoot(String rootPath) {
		sftp.removeCustomRoot(rootPath);
	}

	/**
	 * <p>
	 * Creates a new directory on the remote server. This method will throw an
	 * exception if the directory already exists. To create directories and
	 * disregard any errors use the <code>mkdirs</code> method.
	 * </p>
	 * 
	 * @param dir
	 *            the name of the new directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void mkdir(String dir) throws SftpStatusException, SshException {
		sftp.mkdir(dir);
	}

	/**
	 * <p>
	 * Create a directory or set of directories. This method will not fail even
	 * if the directories exist. It is advisable to test whether the directory
	 * exists before attempting an operation by using <a
	 * href="#stat(java.lang.String)">stat</a> to return the directories
	 * attributes.
	 * </p>
	 * 
	 * @param dir
	 *            the path of directories to create.
	 */
	public void mkdirs(String dir) throws SftpStatusException, SshException {
		sftp.mkdirs(dir);
	}

	/**
	 * Determine whether the file object is pointing to a symbolic link that is
	 * pointing to a directory.
	 * 
	 * @return boolean
	 */
	public boolean isDirectoryOrLinkedDirectory(SftpFile file)
			throws SftpStatusException, SshException {
		return sftp.isDirectoryOrLinkedDirectory(file);
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
		return sftp.pwd();
	}

	/**
	 * <p>
	 * List the contents of the current remote working directory.
	 * </p>
	 * 
	 * <p>
	 * Returns a list of <a
	 * href="../../maverick/ssh2/SftpFile.html">SftpFile</a> instances for the
	 * current working directory.
	 * </p>
	 * 
	 * @return a list of SftpFile for the current working directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * 
	 */
	public SftpFile[] ls() throws SftpStatusException, SshException {
		return sftp.ls();
	}

	/**
	 * <p>
	 * List the contents remote directory.
	 * </p>
	 * 
	 * <p>
	 * Returns a list of <a
	 * href="../../maverick/ssh2/SftpFile.html">SftpFile</a> instances for the
	 * remote directory.
	 * </p>
	 * 
	 * @param path
	 *            the path on the remote server to list
	 * 
	 * @return a list of SftpFile for the remote directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFile[] ls(String path) throws SftpStatusException, SshException {
		return sftp.ls(path);
	}

	/**
	 * <p>
	 * Changes the local working directory.
	 * </p>
	 * 
	 * @param path
	 *            the path to the new working directory
	 * 
	 * @throws SftpStatusException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void lcd(String path) throws SftpStatusException, IOException, PermissionDeniedException {
		sftp.lcd(path);
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
		return sftp.lpwd();
	}

	/**
	 * <p>
	 * Download the remote file to the local computer.
	 * </p>
	 * 
	 * @param path
	 *            the path to the remote file
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
	public SftpFileAttributes get(String path, FileTransferProgress progress)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.get(path, progress);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer.
	 * </p>
	 * 
	 * @param path
	 *            the path to the remote file
	 * @param progress
	 * @param resume
	 *            attempt to resume a interrupted download
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFileAttributes get(String path, FileTransferProgress progress,
			boolean resume) throws SftpStatusException,
			SshException, TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.get(path, progress, resume);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer
	 * 
	 * @param path
	 *            the path to the remote file
	 * @param resume
	 *            attempt to resume an interrupted download
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFileAttributes get(String path, boolean resume)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.get(path, resume);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer
	 * 
	 * @param path
	 *            the path to the remote file
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFileAttributes get(String path) throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.get(path);
	}

	/**
	 * Get the target path of a symbolic link.
	 * 
	 * @param linkpath
	 * @return String
	 * @throws SshException
	 *             if the remote SFTP version is < 3 an exception is thrown as
	 *             this feature is not supported by previous versions of the
	 *             protocol.
	 */
	public String getSymbolicLinkTarget(String linkpath) throws SftpStatusException, SshException {
		return sftp.getSymbolicLinkTarget(linkpath);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer. If the paths provided are
	 * not absolute the current working directory is used.
	 * </p>
	 * 
	 * @param remote
	 *            the path/name of the remote file
	 * @param local
	 *            the path/name to place the file on the local computer
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
	public SftpFileAttributes get(String remote, String local,
			FileTransferProgress progress) throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.get(remote, local, progress);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer. If the paths provided are
	 * not absolute the current working directory is used.
	 * </p>
	 * 
	 * @param remote
	 *            the path/name of the remote file
	 * @param local
	 *            the path/name to place the file on the local computer
	 * @param progress
	 * @param resume
	 *            attempt to resume an interrupted download
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFileAttributes get(String remote, String local,
			FileTransferProgress progress, boolean resume)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {

		return sftp.get(remote, local, progress, resume);
	}
	
	public String getRemoteNewline() throws SftpStatusException {
		return sftp.getRemoteNewline();
	}
	
	public int getRemoteEOL() throws SftpStatusException {
		return sftp.getRemoteEOL();
	}
	
	public int getEOL(String line) throws SftpStatusException {
		return sftp.getEOL(line);
	}
	
	public int getEOL(byte[] nl) throws SftpStatusException {
		return sftp.getEOL(nl);
	}

	/**
	 * Download the remote file into the local file.
	 * 
	 * @param remote
	 * @param local
	 * @param resume
	 *            attempt to resume an interrupted download
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFileAttributes get(String remote, String local, boolean resume)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.get(remote, local, resume);
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
	public SftpFileAttributes get(String remote, String local)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.get(remote, local);
	}

	/**
	 * <p>
	 * Download the remote file writing it to the specified
	 * <code>OutputStream</code>. The OutputStream is closed by this method even
	 * if the operation fails.
	 * </p>
	 * 
	 * @param remote
	 *            the path/name of the remote file
	 * @param local
	 *            the OutputStream to write
	 * @param progress
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public SftpFileAttributes get(String remote, OutputStream local,
			FileTransferProgress progress) throws SftpStatusException,
			SshException, TransferCancelledException {
		return sftp.get(remote, local, progress);
	}

	/**
	 * sets the type of regular expression matching to perform on gets and puts
	 * 
	 * @param syntax
	 *            , NoSyntax for no regular expression matching, GlobSyntax for
	 *            GlobSyntax, Perl5Syntax for Perl5Syntax
	 */
	public void setRegularExpressionSyntax(int syntax) {
		sftp.setRegularExpressionSyntax(syntax);
	}

	/**
	 * Called by getFileMatches() to do regular expression pattern matching on
	 * the files in 'remote''s parent directory.
	 * 
	 * @param remote
	 * @return SftpFile[]
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFile[] matchRemoteFiles(String remote)
			throws SftpStatusException, SshException {
		return sftp.matchRemoteFiles(remote);
	}


	/**
	 * <p>
	 * Download the remote file writing it to the specified
	 * <code>OutputStream</code>. The OutputStream is closed by this method even
	 * if the operation fails.
	 * </p>
	 * 
	 * @param remote
	 *            the path/name of the remote file
	 * @param local
	 *            the OutputStream to write
	 * @param progress
	 * @param position
	 *            the position within the file to start reading from
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public SftpFileAttributes get(String remote, OutputStream local,
			FileTransferProgress progress, long position)
			throws SftpStatusException, SshException,
			TransferCancelledException {
		return sftp.get(remote, local, progress, position);
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
	public InputStream getInputStream(String remotefile, long position)
			throws SftpStatusException, SshException {
		return sftp.getInputStream(remotefile, position);

	}

	/**
	 * Create an InputStream for reading a remote file.
	 * 
	 * @param remotefile
	 * @return InputStream
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public InputStream getInputStream(String remotefile)
			throws SftpStatusException, SshException {
		return sftp.getInputStream(remotefile);
	}

	/**
	 * Download the remote file into an OutputStream.
	 * 
	 * @param remote
	 * @param local
	 * @param position
	 *            the position from which to start reading the remote file
	 * 
	 * @return the downloaded file's attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public SftpFileAttributes get(String remote, OutputStream local,
			long position) throws SftpStatusException, SshException,
			TransferCancelledException {
		return sftp.get(remote, local, position);
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
			throws SftpStatusException, SshException,
			TransferCancelledException {
		return sftp.get(remote, local);
	}

	/**
	 * <p>
	 * Returns the state of the SFTP client. The client is closed if the
	 * underlying session channel is closed. Invoking the <code>quit</code>
	 * method of this object will close the underlying session channel.
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
	 * @param local
	 *            the path/name of the local file
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void put(String local, FileTransferProgress progress, boolean resume)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		sftp.put(local, progress, resume);
	}

	/**
	 * <p>
	 * Upload a file to the remote computer.
	 * </p>
	 * 
	 * @param local
	 *            the path/name of the local file
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void put(String local, FileTransferProgress progress)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		sftp.put(local, progress);
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
	public void put(String local) throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException {
		sftp.put(local);
	}

	/**
	 * Upload a file to the remote computer
	 * 
	 * @param local
	 * @param resume
	 *            attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void put(String local, boolean resume) throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException {
		sftp.put(local, resume);
	}

	/**
	 * <p>
	 * Upload a file to the remote computer. If the paths provided are not
	 * absolute the current working directory is used.
	 * </p>
	 * 
	 * @param local
	 *            the path/name of the local file
	 * @param remote
	 *            the path/name of the destination file
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void put(String local, String remote, FileTransferProgress progress)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		sftp.put(local, remote, progress);
	}

	/**
	 * <p>
	 * Upload a file to the remote computer. If the paths provided are not
	 * absolute the current working directory is used.
	 * </p>
	 * 
	 * @param local
	 *            the path/name of the local file
	 * @param remote
	 *            the path/name of the destination file
	 * @param progress
	 * @param resume
	 *            attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void put(String local, String remote, FileTransferProgress progress,
			boolean resume) throws SftpStatusException,
			SshException, TransferCancelledException, IOException, PermissionDeniedException {
		sftp.put(local, remote, progress, resume);
	}

	/**
	 * Upload a file to the remote computer
	 * 
	 * @param local
	 * @param remote
	 * @param resume
	 *            attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void put(String local, String remote, boolean resume)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		sftp.put(local, remote, resume);
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
	public void put(String local, String remote) throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException {
		sftp.put(local, remote);
	}

	/**
	 * <p>
	 * Upload a file to the remote computer reading from the specified <code>
	 * InputStream</code>. The InputStream is closed, even if the operation
	 * fails.
	 * </p>
	 * 
	 * @param in
	 *            the InputStream being read
	 * @param remote
	 *            the path/name of the destination file
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public void put(InputStream in, String remote, FileTransferProgress progress)
			throws SftpStatusException, SshException,
			TransferCancelledException {
		sftp.put(in, remote, progress);
	}

	public void put(InputStream in, String remote,
			FileTransferProgress progress, long position)
			throws SftpStatusException, SshException,
			TransferCancelledException {
		sftp.put(in, remote, progress, position);
	}

	/**
	 * Create an OutputStream for writing to a remote file.
	 * 
	 * @param remotefile
	 * @return OutputStream
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public OutputStream getOutputStream(String remotefile)
			throws SftpStatusException, SshException {
		return sftp.getOutputStream(remotefile);
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
			throws SftpStatusException, SshException,
			TransferCancelledException {
		sftp.put(in, remote, position);
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
	public void put(InputStream in, String remote) throws SftpStatusException,
			SshException, TransferCancelledException {
		sftp.put(in, remote);
	}

	/**
	 * <p>
	 * Sets the user ID to owner for the file or directory.
	 * </p>
	 * 
	 * @param uid
	 *            numeric user id of the new owner
	 * @param path
	 *            the path to the remote file/directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * 
	 */
	public void chown(String uid, String path) throws SftpStatusException,
			SshException {
		sftp.chown(uid, path);
	}

	/**
	 * <p>
	 * Sets the group ID for the file or directory.
	 * </p>
	 * 
	 * @param gid
	 *            the numeric group id for the new group
	 * @param path
	 *            the path to the remote file/directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void chgrp(String gid, String path) throws SftpStatusException,
			SshException {
		sftp.chgrp(gid, path);
	}

	/**
	 * <p>
	 * Changes the access permissions or modes of the specified file or
	 * directory.
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
	 * @param permissions
	 *            the absolute mode of the file/directory
	 * @param path
	 *            the path to the file/directory on the remote server
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void chmod(int permissions, String path) throws SftpStatusException,
			SshException {
		sftp.chmod(permissions, path);
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
		sftp.umask(umask);
	}

	/**
	 * <p>
	 * Rename a file on the remote computer.
	 * </p>
	 * 
	 * @param oldpath
	 *            the old path
	 * @param newpath
	 *            the new path
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void rename(String oldpath, String newpath)
			throws SftpStatusException, SshException {
		sftp.rename(oldpath, newpath);
	}

	/**
	 * <p>
	 * Remove a file or directory from the remote computer.
	 * </p>
	 * 
	 * @param path
	 *            the path of the remote file/directory
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void rm(String path) throws SftpStatusException, SshException {
		sftp.rm(path);
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
	public void rm(String path, boolean force, boolean recurse)
			throws SftpStatusException, SshException {
		sftp.rm(path, force, recurse);
	}

	/**
	 * <p>
	 * Create a symbolic link on the remote computer.
	 * </p>
	 * 
	 * @param path
	 *            the path to the existing file
	 * @param link
	 *            the new link
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void symlink(String path, String link) throws SftpStatusException,
			SshException {
		sftp.symlink(path, link);
	}

	/**
	 * <p>
	 * Returns the attributes of the file from the remote computer.
	 * </p>
	 * 
	 * @param path
	 *            the path of the file on the remote computer
	 * 
	 * @return the attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFileAttributes stat(String path) throws SftpStatusException,
			SshException {
		return sftp.stat(path);
	}
	
	/**
	 * <p>
	 * Returns the attributes of the link from the remote computer.
	 * </p>
	 * 
	 * @param path
	 *            the path of the file on the remote computer
	 * 
	 * @return the attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFileAttributes statLink(String path) throws SftpStatusException,
			SshException {
		return sftp.statLink(path);
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
	public String getAbsolutePath(String path) throws SftpStatusException,
			SshException {
		return sftp.getAbsolutePath(path);
	}

	/**
	 * Verify a local and remote file. Requires a minimum SFTP version of 5 and/or support of the "md5-hash" extension
	 * @param localFile
	 * @param remoteFile
	 * @return
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public boolean verifyFiles(String localFile, String remoteFile) throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		return sftp.verifyFiles(localFile, remoteFile);
	}
	
	/**
	 * Verify a local and remote file. Requires a minimum SFTP version of 5 and/or support of the "md5-hash" extension.
	 * 
	 * @param localFile
	 * @param remoteFile
	 * @param offset
	 * @param length
	 * @return
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public boolean verifyFiles(String localFile, String remoteFile, long offset, long length) throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		return sftp.verifyFiles(localFile, remoteFile, offset, length);
	}
	/**
	 * <p>
	 * Close the SFTP client.
	 * </p>
	 * 
	 */
	public void quit() throws SshException {
		sftp.quit();
	}

	/**
	 * <p>
	 * Close the SFTP client.
	 * </p>
	 * 
	 */
	public void exit() throws SshException {
		sftp.exit();
	}

	/**
	 * Copy the contents of a local directory into a remote directory.
	 * 
	 * @param localdir
	 *            the path to the local directory
	 * @param remotedir
	 *            the remote directory which will receive the contents
	 * @param recurse
	 *            recurse through child folders
	 * @param sync
	 *            synchronize the directories by removing files on the remote
	 *            server that do not exist locally
	 * @param commit
	 *            actually perform the operation. If <tt>false</tt> a <a
	 *            href="DirectoryOperation.html">DirectoryOperation</a> will be
	 *            returned so that the operation can be evaluated and no actual
	 *            files will be created/transfered.
	 * @param progress
	 * 
	 * @return DirectoryOperation
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public DirectoryOperation putLocalDirectory(String localdir,
			String remotedir, boolean recurse, boolean sync, boolean commit,
			FileTransferProgress progress) throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.putLocalDirectory(localdir, remotedir, recurse, sync, commit, progress);
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
	public static String formatLongname(SftpFile file)
			throws SftpStatusException, SshException {
		return SftpClient.formatLongname(file);
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
	public static String formatLongname(SftpFileAttributes attrs,
			String filename) {
		return SftpClient.formatLongname(attrs, filename);
	}


	/**
	 * Copy the contents of a remote directory to a local directory
	 * 
	 * @param remotedir
	 *            the remote directory whose contents will be copied.
	 * @param localdir
	 *            the local directory to where the contents will be copied
	 * @param recurse
	 *            recurse into child folders
	 * @param sync
	 *            synchronized the directories by removing files and directories
	 *            that do not exist on the remote server.
	 * @param commit
	 *            actually perform the operation. If <tt>false</tt> the
	 *            operation will be processed and a <a
	 *            href="DirectoryOperation.html">DirectoryOperation</a> will be
	 *            returned without actually transfering any files.
	 * @param progress
	 * 
	 * @return DirectoryOperation
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public DirectoryOperation getRemoteDirectory(String remotedir,
			String localdir, boolean recurse, boolean sync, boolean commit,
			FileTransferProgress progress) throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.getRemoteDirectory(remotedir, localdir, recurse, sync, commit, progress);
	}

	/**
	 * <p>
	 * Download the remote files to the local computer
	 * </p>
	 * 
	 * <p>
	 * When RegExpSyntax is set to NoSyntax the getFiles() methods act
	 * identically to the get() methods except for a different return type.
	 * </p>
	 * 
	 * <p>
	 * When RegExpSyntax is set to GlobSyntax or Perl5Syntax, getFiles() treats
	 * 'remote' as a regular expression, and gets all the files in 'remote''s
	 * parent directory that match the pattern. The default parent directory of
	 * remote is the remote cwd unless 'remote' contains file seperators(/).
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
	 * @param remote
	 *            the regular expression path to the remote file
	 * 
	 * @return the downloaded files' attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFile[] getFiles(String remote) throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.getFiles(remote);
	}

	/**
	 * <p>
	 * Download the remote files to the local computer
	 * 
	 * @param remote
	 *            the regular expression path to the remote file
	 * @param resume
	 *            attempt to resume an interrupted download
	 * 
	 * @return the downloaded files' attributes
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFile[] getFiles(String remote, boolean resume)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.getFiles(remote, resume);
	}

	/**
	 * <p>
	 * Download the remote files to the local computer.
	 * </p>
	 * 
	 * @param remote
	 *            the regular expression path to the remote file
	 * @param progress
	 * 
	 * @return SftpFile[]
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFile[] getFiles(String remote, FileTransferProgress progress)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.getFiles(remote, progress);
	}

	/**
	 * <p>
	 * Download the remote files to the local computer.
	 * </p>
	 * 
	 * @param remote
	 *            the regular expression path to the remote file
	 * @param progress
	 * @param resume
	 *            attempt to resume a interrupted download
	 * 
	 * @return SftpFile[]
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFile[] getFiles(String remote, FileTransferProgress progress,
			boolean resume) throws SftpStatusException,
			SshException, TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.getFiles(remote, progress, resume);
	}

	/**
	 * Download the remote files into the local file.
	 * 
	 * @param remote
	 * @param local
	 * 
	 * @return SftpFile[]
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFile[] getFiles(String remote, String local)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.getFiles(remote, local);
	}

	/**
	 * Download the remote files into the local file.
	 * 
	 * @param remote
	 * @param local
	 * @param resume
	 *            attempt to resume an interrupted download
	 * 
	 * @return SftpFile[]
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFile[] getFiles(String remote, String local, boolean resume)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.getFiles(remote, local, resume);
	}

	/**
	 * <p>
	 * Download the remote file to the local computer. If the paths provided are
	 * not absolute the current working directory is used.
	 * </p>
	 * 
	 * @param remote
	 *            the regular expression path/name of the remote files
	 * @param local
	 *            the path/name to place the file on the local computer
	 * @param progress
	 * 
	 * @return SftpFile[]
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public SftpFile[] getFiles(String remote, String local,
			FileTransferProgress progress, boolean resume)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		return sftp.getFiles(remote, local, progress, resume);
	}

	/**
	 * <p>
	 * Upload the contents of an InputStream to the remote computer.
	 * </p>
	 * 
	 * <p>
	 * When RegExpSyntax is set to NoSyntax the putFiles() methods act
	 * identically to the put() methods except for a different return type.
	 * </p>
	 * 
	 * <p>
	 * When RegExpSyntax is set to GlobSyntax or Perl5Syntax, putFiles() treats
	 * 'local' as a regular expression, and gets all the files in 'local''s
	 * parent directory that match the pattern. The default parent directory of
	 * local is the local cwd unless 'local' contains file seperators.
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
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void putFiles(String local) throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException {
		sftp.putFiles(local);
	}

	/**
	 * Upload files to the remote computer
	 * 
	 * @param local
	 * @param resume
	 *            attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void putFiles(String local, boolean resume)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		sftp.putFiles(local, resume);
	}

	/**
	 * <p>
	 * Upload files to the remote computer
	 * </p>
	 * 
	 * @param local
	 *            the regular expression path/name of the local files
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void putFiles(String local, FileTransferProgress progress)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		sftp.putFiles(local, progress);
	}

	/**
	 * <p>
	 * Upload files to the remote computer
	 * </p>
	 * 
	 * @param local
	 *            the regular expression path/name of the local files
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void putFiles(String local, FileTransferProgress progress,
			boolean resume) throws SftpStatusException,
			SshException, TransferCancelledException, IOException, PermissionDeniedException {
		sftp.putFiles(local, progress, resume);
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
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void putFiles(String local, String remote)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		sftp.putFiles(local, remote);
	}

	/**
	 * Upload files to the remote computer
	 * 
	 * @param local
	 * @param remote
	 * @param resume
	 *            attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void putFiles(String local, String remote, boolean resume)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		sftp.putFiles(local, remote, resume);
	}

	/**
	 * <p>
	 * Upload files to the remote computer. If the paths provided are not
	 * absolute the current working directory is used.
	 * </p>
	 * 
	 * @param local
	 *            the regular expression path/name of the local files
	 * @param remote
	 *            the path/name of the destination file
	 * @param progress
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void putFiles(String local, String remote,
			FileTransferProgress progress) throws SftpStatusException, SshException, TransferCancelledException, IOException, PermissionDeniedException {
		sftp.putFiles(local, remote, progress);
	}

	/**
	 * make local copies of some of the variables, then call putfilematches,
	 * which calls "put" on each file that matches the regexp local.
	 * 
	 * @param local
	 *            the regular expression path/name of the local files
	 * @param remote
	 *            the path/name of the destination file
	 * @param progress
	 * @param resume
	 *            attempt to resume after an interrupted transfer
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 */
	public void putFiles(String local, String remote,
			FileTransferProgress progress, boolean resume)
			throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException {
		sftp.putFiles(local, remote, progress, resume);
	}
}
