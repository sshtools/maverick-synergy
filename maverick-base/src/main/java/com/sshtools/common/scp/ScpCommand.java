

package com.sshtools.common.scp;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.InvalidHandleException;
import com.sshtools.common.sftp.SftpFile;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;
import com.sshtools.common.util.Utils;

/**
 * Provides support for the SCP command. To enable this support add this class
 * in the
 * {@link com.maverick.sshd.SshDaemon#configure(com.maverick.sshd.ConfigurationContext)}
 * method using the following code<br>
 * <br>
 * <blockquote>
 * 
 * <pre>
 * context.addCommand(&quot;scp&quot;, ScpCommand.class);
 * </pre>
 * 
 * </blockquote>
 * 
 */
public class ScpCommand extends ExecutableCommand implements Runnable {
	
	private static int BUFFER_SIZE = 16384;

	// Private instance variables
	private String destination;
	private int verbosity = 0;
	private int exitCode = ExecutableCommand.STILL_ACTIVE;
	private boolean directory;
	private boolean recursive;
	private boolean from;
	private boolean to;
	private AbstractFileSystem nfs;
	private byte[] buffer = new byte[BUFFER_SIZE];
	private boolean preserveAttributes;
	private boolean firstPath = true;
	
	private String currentDirectory;
	private FileSystemPolicy filePolicy;
	
	public ScpCommand(){
	    this(".");
	}
	
	/**
	 * Creates a new ScpCommand object.
	 */
	public ScpCommand(String currentDirectory) {
		this.currentDirectory = currentDirectory;
	}

	/**
	 * Parse the SCP command line and configure the command ready for execution.
	 * 
	 * @param command
	 * @param environment
	 * @return boolean
	 */
	public boolean createProcess(String[] args, Map<String,String> environment) {

		String command = Utils.mergeToArgsString(args);
		
		if(Log.isDebugEnabled())
			Log.debug("Creating SCP with command line '{}' and current working directory '{}'", command, currentDirectory);

		try {

			nfs = new AbstractFileSystem(
							session.getConnection(),
							AbstractFileSystem.SCP);

			scp(Arrays.copyOfRange(args, 1, args.length));

			return true;
		} catch (IOException ex) {
			if(Log.isDebugEnabled())
				Log.debug("Failed to start command: {}", ex, command);
		} catch (Throwable t) {
			if(Log.isDebugEnabled())
				Log.debug("SCP command could not be processed: {}", t, command);
		}

		return false;
	}

	public int getExitCode() {
		return exitCode;
	}

	/**
	 * Called when channel is closing
	 */
	public void kill() {
		if(Log.isDebugEnabled())
			Log.debug("Killing SCP command");

		// Closing the streams will force any blocking reads to return
		try {
			getInputStream().close();
		} catch (IOException ex) {
		}

		try {
			getOutputStream().close();
		} catch (IOException ex) {
		}
	}

	/**
	 * Start the process.
	 * 
	 * @throws IOException
	 */
	public void onStart() {

		if(Log.isDebugEnabled()) {
			Log.debug("Adding SCP command to executor service");
		}
		session.getConnection().executeTask(new ConnectionAwareTask(session.getConnection()) {
			protected void doTask() {
				ScpCommand.this.run();
			}
		});
		
	}

	/**
	 * Parse the SCP command line and configure the class ready for execution.
	 * 
	 * @param args
	 * @throws IOException
	 */
	private void scp(String[] a) throws IOException {

		// Parse the command line for supported options
		destination = null;
		directory = false;
		from = false;
		to = false;
		recursive = false;
		verbosity = 0;

		int pathIndex = 0;
		for(int i=0;i<a.length;i++) {
			if(!a[i].startsWith("-")) {
				break;
			}
			pathIndex++;
		}
		
		for (int i = 0; i < pathIndex; i++) {
			
				String s = a[i].substring(1);

				for (int j = 0; j < s.length(); j++) {
					char ch = s.charAt(j);

					switch (ch) {
					case 't':
						to = true;

						continue;

					case 'd':
						directory = true;

						continue;

					case 'f':
						from = true;

						continue;

					case 'r':
						recursive = true;

						continue;

					case 'v':
						verbosity++;

						continue;

					case 'p':
						preserveAttributes = true;

						continue;

					default:
						if(Log.isDebugEnabled())
							Log.debug("Unsupported SCP argument {}", ch);
					}
				}
			}
	
			for(int i=pathIndex;i<a.length;i++) {
				if (destination == null) {
					destination = a[i];
				} else {
	
					if (destination.endsWith("\\")) {
						destination = destination.substring(0,
								destination.length() - 1);
					} 
					destination += " " + a[i];
				}
			}
			
		

		if (!to && !from) {
			throw new IOException("Must supply either -t or -f.");
		}

		if (destination == null) {
			throw new IOException("Destination not supplied.");
		}
		destination = destination.trim();
        if (destination.startsWith("\"") && destination.endsWith("\"")){
            destination = destination.substring(1, destination.length() - 1);
        }
        if (destination.startsWith("\'") && destination.endsWith("\'")){
            destination = destination.substring(1, destination.length() - 1);
        }

		if(Log.isDebugEnabled())
			Log.debug("Destination is {}", destination);
		if(Log.isDebugEnabled())
			Log.debug("Recursive is {}", recursive);
		if(Log.isDebugEnabled())
			Log.debug("Directory is {}", directory);
		if(Log.isDebugEnabled())
			Log.debug("Verbosity is {}", verbosity);
		if(Log.isDebugEnabled())
			Log.debug("Sending files is {}", from);
		if(Log.isDebugEnabled())
			Log.debug("Receiving files is {}", to);
		if(Log.isDebugEnabled())
			Log.debug("Preserve Attributes {}", preserveAttributes);

	}

	

	/**
	 * Send ok command to client
	 * 
	 * @throws IOException
	 *             on any error
	 */
	private void writeOk() throws IOException {
		if(Log.isDebugEnabled())
			Log.debug("Sending client OK command");
		getOutputStream().write(0);
	}

	/**
	 * Send command to client
	 * 
	 * @param cmd
	 *            command
	 * 
	 * @throws IOException
	 *             on any error
	 */
	private void writeCommand(String cmd) throws IOException {
		if(Log.isDebugEnabled())
			Log.debug("Sending command '{}'", cmd);
		getOutputStream().write(cmd.getBytes());

		if (!cmd.endsWith("\n")) {
			getOutputStream().write("\n".getBytes());
		}
	}

	/**
	 * Send error message to client
	 * 
	 * @param msg
	 *            error message
	 * 
	 * @throws IOException
	 *             on any error
	 */
	private void writeError(String msg) throws IOException {
		writeError(msg, false);
	}

	/**
	 * Send error message to client
	 * 
	 * @param msg
	 *            error message
	 * @param serious
	 *            serious error
	 * 
	 * @throws IOException
	 *             on any error
	 */
	private void writeError(String msg, boolean serious) throws IOException {

		exitCode = 1;

		if (session.isClosed()) {
			if(Log.isDebugEnabled())
				Log.debug("SCP received error '{}' but session is closed so cannot inform client", msg);
		} else {
			if(Log.isDebugEnabled())
				Log.debug("Sending error message '{}' to client (serious={})", msg, serious);

			getOutputStream().write(serious ? (byte) 2 : (byte) 1);
			getOutputStream().write(msg.getBytes());

			if (!msg.endsWith("\n")) {
				getOutputStream().write("\n".getBytes());
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if(Log.isDebugEnabled())
			Log.debug("SCP thread has started");

		try {
			if (from) {
				if(Log.isDebugEnabled())
					Log.debug("SCP is sending files to client");

				try {
					waitForResponse();

					String base = destination;
					String dir = currentDirectory;
					if (destination.startsWith("/")){
					    dir = "";
					}
					int idx = base.lastIndexOf('/');

					if (idx != -1) {
						if (idx > 0) {
							dir = base.substring(0, idx);
						}

						base = base.substring(idx + 1);
					}

					if(Log.isDebugEnabled()) {
						Log.debug("Looking for matches in {} for {}", dir, base);
					}
					
					// Build a string pattern that may be used to match
					// wildcards
					PathMatcher matcher =
						    FileSystems.getDefault().getPathMatcher("glob:" + base);

					byte[] handle = null;
					boolean eof = false;
					boolean found = false;
					
					try {
						handle = nfs.openDirectory(dir);
						
						while (!eof) {
							try {
								SftpFile[] files = nfs.readDirectory(handle);

								for (int i = 0; i < files.length; i++) {
									if(Log.isDebugEnabled())
										Log.debug("Testing for match against {}", files[i].getFilename());

									if (!files[i].getFilename().equals(".")
											&& !files[i].getFilename().equals(
													"..")) {
										if (matcher.matches(Paths.get(files[i].getFilename()))) {
											if(Log.isDebugEnabled())
												Log.debug("Matched");
											found = true;
											writeFileToRemote(dir + "/"
													+ files[i].getFilename());
										} else {
											if(Log.isDebugEnabled())
												Log.debug("No match");

										}
									}
								}
							} catch (EOFException e) {
								eof = true;
							}
						}
					} finally {
						if (handle != null) {
							try {
								nfs.closeFile(handle);
							} catch (InvalidHandleException e) {
							}
						}
						
						if(eof && !found) {
							writeError(base + " not found", true);
						}
					}

				} catch (FileNotFoundException fnfe) {
					if(Log.isDebugEnabled())
						Log.debug("", fnfe);
					writeError(fnfe.getMessage(), true);
				} catch (PermissionDeniedException pde) {
					if(Log.isDebugEnabled())
						Log.debug("", pde);
					writeError(pde.getMessage(), true);
				} catch (InvalidHandleException ihe) {
					if(Log.isDebugEnabled())
						Log.debug("", ihe);
					writeError(ihe.getMessage(), true);
				} catch (IOException ioe) {
					if(Log.isDebugEnabled())
						Log.debug("", ioe);
					writeError(ioe.getMessage(), true);
				}
			} else {
				if(Log.isDebugEnabled())
					Log.debug("SCP is receiving files from the client");
				readFromRemote(destination);
			}

			exitCode = 0;
		} catch (Throwable t) {
			if(Log.isDebugEnabled())
				Log.debug("SCP thread failed", t);
			exitCode = 1;
		}

		if(Log.isDebugEnabled())
			Log.debug("SCP thread is exiting");
		
		// Close the filesystem
		nfs.closeFilesystem();

		closeSession();

		
	}

    protected void closeSession() {
        if(Log.isDebugEnabled()){
            Log.debug("Closing session");
        }
        if (!session.isClosed()){
			session.close();
        }
    }

	private boolean writeDirToRemote(String path) throws IOException {

		byte[] handle = null;
		try {

			SftpFileAttributes attr = nfs.getFileAttributes(path);

			if (attr.isDirectory() && !recursive) {
				writeError("File " + path
						+ " is a directory, use recursive mode");

				return false;
			}

			String basename = path;
			int idx = path.lastIndexOf('/');

			if (idx != -1) {
				basename = path.substring(idx + 1);
			}

			writeCommand("D" + attr.getMaskString() + " 0 " + basename + "\n");
			waitForResponse();

			handle = nfs.openDirectory(path);

			SftpFile[] list;

			try {

				do {
					list = nfs.readDirectory(handle);

					for (int i = 0; i < list.length; i++) {
						if (!list[i].getFilename().equals(".")
								&& !list[i].getFilename().equals("..")) {
							writeFileToRemote(path + "/"
									+ list[i].getFilename());
						}
					}
				} while (list.length > 0);

			} catch (EOFException ex) {
				// No more directories
			}

			writeCommand("E");

			waitForResponse();

		} catch (InvalidHandleException ihe) {
			throw new IOException(ihe.getMessage());
		} catch (PermissionDeniedException e) {
			throw new IOException(e.getMessage());
		} finally {
			if (handle != null) {
				try {
					nfs.closeFile(handle);
				} catch (Exception e) {
					if(Log.isDebugEnabled())
						Log.debug("", e);
				}
			}
		}

		return true;
	}

	private void writeFileToRemote(String path) throws IOException,
			PermissionDeniedException, InvalidHandleException {
		SftpFileAttributes attr = nfs.getFileAttributes(path);

		if (attr.isDirectory()) {
			if (!writeDirToRemote(path)) {
				return;
			}
		} else if (attr.isFile()) {
			String basename = path;
			int idx = basename.lastIndexOf('/');

			if (idx != -1) {
				basename = path.substring(idx + 1);
			}

			writeCommand("C" + attr.getMaskString() + " " + attr.getSize()
					+ " " + basename + "\n");
			
			waitForResponse();

			Date started = new Date();
			
			path = nfs.getRealPath(path);

			if(Log.isDebugEnabled())
				Log.debug("Opening file {}", path);

			fireEvent(
					new Event(
							this,
							EventCodes.EVENT_SCP_DOWNLOAD_INIT,
							true)
							.addAttribute(
									EventCodes.ATTRIBUTE_FILE_NAME,
									path)
							.addAttribute(
									EventCodes.ATTRIBUTE_OPERATION_STARTED,
									started)
							.addAttribute(
									EventCodes.ATTRIBUTE_OPERATION_FINISHED,
									new Date())
							.addAttribute(
									EventCodes.ATTRIBUTE_BYTES_EXPECTED,
									new Long(attr.getSize().longValue()))
							.addAttribute(
									EventCodes.ATTRIBUTE_FILE_FACTORY,
									nfs.getFileFactory())
							.addAttribute(
									EventCodes.ATTRIBUTE_CONNECTION,
									session.getConnection()));
			byte[] handle = null;
			long count = 0;

			try {
				handle = nfs.openFile(path, new UnsignedInteger32(
						AbstractFileSystem.OPEN_READ), attr);

				if(Log.isDebugEnabled())
					Log.debug("Sending file");

				fireEvent(
						new Event(
								this,
								EventCodes.EVENT_SCP_DOWNLOAD_STARTED,
								true)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										path)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date())
								.addAttribute(
										EventCodes.ATTRIBUTE_BYTES_EXPECTED,
										new Long(attr.getSize().longValue()))
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_FACTORY,
										nfs.getFileFactory())
								.addAttribute(
										EventCodes.ATTRIBUTE_HANDLE,
										handle)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										session.getConnection()));
				
				UnsignedInteger64 offset = new UnsignedInteger64(count);
				
				byte[] buf = null;
				
				while (count < attr.getSize().longValue()) {

					
					try {
						buf = new byte[BUFFER_SIZE];
						int read = nfs.readFile(handle, offset, buf, 0,
								buf.length);
						if (read < 0)
							break;
						offset = UnsignedInteger64.add(offset, read);
						count += read;
						if(Log.isDebugEnabled())
							Log.debug("Writing block of {} bytes", read);
						getOutputStream().write(buf, 0, read);
						

						if(session.getConnection().getContext().getPolicy(ScpPolicy.class).isSCPReadWriteEvents()) {
							fireEvent(new Event(
									this,
									EventCodes.EVENT_SCP_FILE_READ,
									true)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											session.getConnection())
									.addAttribute(
											EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
											new Long(count))
									.addAttribute(
											EventCodes.ATTRIBUTE_BYTES_READ,
											new Long(read))
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_NAME,
											path)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_STARTED,
											started)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_FINISHED,
											new Date())
									.addAttribute(
											EventCodes.ATTRIBUTE_HANDLE,
											handle)
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_FACTORY,
											nfs.getFileFactory()));
						}
						

					} catch (EOFException eofe) {
						if(Log.isDebugEnabled())
							Log.debug("End of file - finishing transfer");
						break;
					}
				}

				// pipeIn.flush();

				if (count < attr.getSize().longValue()) {
					throw new IOException(
							"File transfer terminated abnormally.");
				}


				fireEvent(
						new Event(
								this,
								EventCodes.EVENT_SCP_DOWNLOAD_COMPLETE,
								true)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										path)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date())
								.addAttribute(
										EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
										new Long(count))
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_FACTORY,
										nfs.getFileFactory())
								.addAttribute(
										EventCodes.ATTRIBUTE_HANDLE,
										handle)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										session.getConnection()));
				
				writeOk();

				waitForResponse();

			} catch(Throwable ex) { 
				if(Log.isErrorEnabled()){
					Log.error("Write to remote failed", ex);
				}
				fireDownloadErrorEvent(handle, path, started, count, ex, 
						session.getConnection());
			} finally {
				if (handle != null) {
					try {
						nfs.closeFile(handle);
					} catch (Exception e) {
						if(Log.isDebugEnabled())
							Log.debug("", e);
					}
				}
			
			}
		} else {
			throw new IOException(path + " not valid for SCP.");
		}

		exitCode = 0;
	}

	private void fireDownloadErrorEvent(byte[] handle, String path, Date started, long count, Throwable ex, SshConnection con) {
		fireEvent(
				new Event(
						this,
						EventCodes.EVENT_SCP_DOWNLOAD_COMPLETE,
						ex)
						.addAttribute(
								EventCodes.ATTRIBUTE_FILE_NAME,
								path)
						.addAttribute(
								EventCodes.ATTRIBUTE_OPERATION_STARTED,
								started)
						.addAttribute(
								EventCodes.ATTRIBUTE_OPERATION_FINISHED,
								new Date())
						.addAttribute(
								EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
								new Long(count))
						.addAttribute(
								EventCodes.ATTRIBUTE_FILE_FACTORY,
								nfs.getFileFactory())
						.addAttribute(
								EventCodes.ATTRIBUTE_HANDLE,
								handle)
						.addAttribute(
								EventCodes.ATTRIBUTE_CONNECTION,
								session.getConnection()));
	}
	
	private void fireEvent(Event event) {
		nfs.populateEvent(event);
		EventServiceImplementation.getInstance().fireEvent(event);
	}

	private void waitForResponse() throws IOException {
		if(Log.isDebugEnabled())
			Log.debug("Waiting for response");

		int r = getInputStream().read();

		if (r == 0) {
			if(Log.isDebugEnabled())
				Log.debug("Got OK");

			// All is well, no error
			return;
		}

		if (r == -1) {
			throw new EOFException("SCP returned unexpected EOF");
		}

		String msg = readString();
		if(Log.isDebugEnabled())
			Log.debug("Got error '{}'", msg);

		if (r == (byte) '\02') {
			if(Log.isDebugEnabled())
				Log.debug("This is a serious error");
			throw new IOException(msg);
		}

		throw new IOException("SCP returned an unexpected error: " + msg);
	}

	private void readFromRemote(String path) throws IOException {
		String cmd;
		String[] cmdParts = new String[3];
		writeOk();

		while (!session.isClosed() && !session.isRemoteEOF()) {
			if(Log.isDebugEnabled())
				Log.debug("Waiting for command");

			try {
				cmd = readString();
				exitCode = ExecutableCommand.STILL_ACTIVE;
			} catch (EOFException e) {
				return;
			}

			if(Log.isDebugEnabled())
				Log.debug("Got command '{}'", cmd);

			char cmdChar = cmd.charAt(0);

			switch (cmdChar) {
			case 'E':
				writeOk();

				return;

			case 'T':
				if(Log.isDebugEnabled())
					Log.debug("SCP time not currently supported");

				writeOk();
				// waitForResponse();

				// writeError(
				// "WARNING: This server does not currently support the SCP time command");
				// exitCode = 1;
				break;

			case 'C':
			case 'D':
				parseCommand(cmd, cmdParts);

				String name = cmdParts[2];
				String targetPath;

				SftpFileAttributes targetAttr = null;

				try {
					targetAttr = nfs.getFileAttributes(path);
				} catch (FileNotFoundException ex) {
					if(Log.isDebugEnabled())
						Log.debug("File {} not found", path);
				} catch (PermissionDeniedException ex) {
					if(Log.isDebugEnabled())
						Log.debug("File {} permission denied!", path);
				}

				if (cmdChar == 'D') {

					if(Log.isDebugEnabled())
						Log.debug("Got directory request");

					if (path.equals("."))
						targetPath = name;
					else if (targetAttr == null && firstPath)
						targetPath = path;
					else
						targetPath = path + (path.endsWith("/") ? "" : "/")
								+ name;

					firstPath = false;

					try {
						targetAttr = nfs.getFileAttributes(targetPath);
					} catch (FileNotFoundException ex) {
						if(Log.isDebugEnabled())
							Log.debug("File {} not found", targetPath);
						targetAttr = null;
					} catch (PermissionDeniedException ex) {
						if(Log.isDebugEnabled())
							Log.debug("File {} permission denied", targetPath);
						targetAttr = null;
					}

					if (targetAttr != null) {
						if (!targetAttr.isDirectory()) {
							String msg = "Invalid target " + name
									+ ", must be a directory";
							writeError(msg);
							throw new IOException(msg);
						}
					} else {
						try {
							if(Log.isDebugEnabled())
								Log.debug("Creating directory {}", targetPath);

							if (!nfs.makeDirectory(targetPath, new SftpFileAttributes(
									SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY,
									getSession().getConnection().getContext().getPolicy(ScpPolicy.class).getSCPCharsetEncoding()))) {
								String msg = "Could not create directory: "
										+ name;
								writeError(msg);
								throw new IOException(msg);
							}
							targetAttr = nfs.getFileAttributes(targetPath);
							if(Log.isDebugEnabled())
								Log.debug("Setting permissions on directory");
							targetAttr
									.setPermissionsFromMaskString(cmdParts[0]);
						} catch (FileNotFoundException e1) {
							writeError("File not found");
							throw new IOException("File not found");
						} catch (PermissionDeniedException e1) {
							writeError("Permission denied");
							throw new IOException("Permission denied");
						}
					}

					readFromRemote(targetPath);

					// Everything went ok, set the exit code
					exitCode = 0;
					continue;
				}

				if (targetAttr == null || !targetAttr.isDirectory()) {
					targetPath = path;
				} else {
					targetPath = path + (path.endsWith("/") ? "" : "/") + name;
				}
				if (targetAttr == null) {
					targetAttr = new SftpFileAttributes(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR, "UTF-8");
				}

				targetAttr.setSize(new UnsignedInteger64(cmdParts[1]));

				byte[] handle = null;
				long length = 0;
				Date started = new Date();
				long count = 0;
				
				SshConnection con = session.getConnection();
				
				fireEvent(
						new Event(
								this,
								EventCodes.EVENT_SCP_UPLOAD_INIT,
								true)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										targetPath)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date())
								.addAttribute(
										EventCodes.ATTRIBUTE_BYTES_EXPECTED,
										new Long(length))
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_FACTORY,
										nfs.getFileFactory())
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										con));

				try {
					targetPath = nfs.getRealPath(targetPath);
					
					if(Log.isDebugEnabled())
						Log.debug("Opening file for writing {}", targetPath);
					
					// Open the file
					handle = nfs.openFile(targetPath, new UnsignedInteger32(
							AbstractFileSystem.OPEN_CREATE
									| AbstractFileSystem.OPEN_WRITE
									| AbstractFileSystem.OPEN_TRUNCATE),
							targetAttr);
					if(Log.isDebugEnabled())
						Log.debug("NFS file opened");
					writeOk();
					
					int read;
					length = Long.parseLong(cmdParts[1]);
					
					fireEvent(
							new Event(
									this,
									EventCodes.EVENT_SCP_UPLOAD_STARTED,
									true)
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_NAME,
											targetPath)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_STARTED,
											started)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_FINISHED,
											new Date())
									.addAttribute(
											EventCodes.ATTRIBUTE_BYTES_EXPECTED,
											new Long(length))
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_FACTORY,
											nfs.getFileFactory())
									.addAttribute(
											EventCodes.ATTRIBUTE_HANDLE,
											handle)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											con));

					
					if(Log.isDebugEnabled())
						Log.debug("Reading from client");
					
					if(filePolicy != null && filePolicy.hasUploadQuota()) {
						if(!con.containsProperty("uploadQuota")) {
							con.setProperty("uploadQuota", new Long(0L));
						}
						Long quota = (Long) con.getProperty("uploadQuota");
						if(quota + length > filePolicy.getConnectionUploadQuota()) {
							writeError("User quota will be exceeded");
							throw new IOException("User quota will be exceeded");
						}
						
						con.setProperty("uploadQuota", new Long(quota + length));
					}
					
					UnsignedInteger64 offset = new UnsignedInteger64(0);

					while (count < length) {
						read = getInputStream()
								.read(buffer,
										0,
										(int) (((length - count) < buffer.length) ? (length - count)
												: buffer.length));

						if (read == -1) {
							throw new EOFException(
									"Scp received an unexpected EOF during file transfer");
						}

						if(Log.isDebugEnabled())
							Log.debug("Got block of {} bytes", read);
						nfs.writeFile(handle, offset, buffer, 0, read);
						offset = UnsignedInteger64.add(offset, read);
						count += read;
						
						if(session.getConnection().getContext().getPolicy(ScpPolicy.class).isSCPReadWriteEvents()) {
							fireEvent(new Event(
									this,
									EventCodes.EVENT_SCP_FILE_WRITE,
									true)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											con)
									.addAttribute(
											EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
											new Long(count))
									.addAttribute(
											EventCodes.ATTRIBUTE_BYTES_WRITTEN,
											new Long(read))
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_NAME,
											targetPath)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_STARTED,
											started)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_FINISHED,
											new Date())
									.addAttribute(
											EventCodes.ATTRIBUTE_HANDLE,
											handle) 
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_FACTORY,
											nfs.getFileFactory()));
						}
					}

					fireEvent(
							new Event(
									this,
									EventCodes.EVENT_SCP_UPLOAD_COMPLETE,
									true)
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_NAME,
											targetPath)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_STARTED,
											started)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_FINISHED,
											new Date())
									.addAttribute(
											EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
											new Long(count))
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_FACTORY,
											nfs.getFileFactory())
									.addAttribute(
											EventCodes.ATTRIBUTE_HANDLE,
											handle)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											con));
					
				} catch (InvalidHandleException ex) {
					writeError("Invalid handle.");
					fireUploadErrorEvent(handle, targetPath, started, count, ex, con);
					throw new IOException("Invalid handle.");
				} catch (FileNotFoundException ex) {
					writeError("File not found");
					fireUploadErrorEvent(handle, targetPath, started, count, ex, con);
					throw new IOException("File not found");
				} catch (PermissionDeniedException ex) {
					writeError("Permission denied");
					fireUploadErrorEvent(handle, targetPath, started, count, ex, con);
					throw new IOException("Permission denied");
				} catch (Throwable ex) {
					writeError("Received exception during transfer to file system. " + ex.getMessage());
					fireUploadErrorEvent(handle, targetPath, started, count, ex, con);
					throw new IOException(ex.getMessage(), ex);
				} finally {
					if (handle != null) {
						try {
							if(Log.isDebugEnabled())
								Log.debug("Closing handle");
							nfs.closeFile(handle);
						} catch (Exception e) {
						}
					}
				}

				waitForResponse();

				if (preserveAttributes) {
					targetAttr.setPermissionsFromMaskString(cmdParts[0]);
					if(Log.isDebugEnabled())
						Log.debug("Setting permissions on directory to {}", targetAttr.getPermissionsString());

					try {
						nfs.setFileAttributes(targetPath, targetAttr);
					} catch (Exception e) {
						writeError("Failed to set file permissions.");

						break;
					}
				}

				writeOk();
				exitCode = 0;

				break;

			default:
				writeError("Unexpected cmd: " + cmd);
				throw new IOException("SCP unexpected cmd: " + cmd);
			}
		}
	}

	private void fireUploadErrorEvent(byte[] handle, String targetPath, Date started, long count, Throwable ex, SshConnection con) {
		fireEvent(
				new Event(
						this,
						EventCodes.EVENT_SCP_UPLOAD_COMPLETE,
						ex)
						.addAttribute(
								EventCodes.ATTRIBUTE_FILE_NAME,
								targetPath)
						.addAttribute(
								EventCodes.ATTRIBUTE_OPERATION_STARTED,
								started)
						.addAttribute(
								EventCodes.ATTRIBUTE_OPERATION_FINISHED,
								new Date())
						.addAttribute(
								EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
								new Long(count))
						.addAttribute(
								EventCodes.ATTRIBUTE_FILE_FACTORY,
								nfs.getFileFactory())
						.addAttribute(
								EventCodes.ATTRIBUTE_HANDLE,
								handle)
						.addAttribute(
								EventCodes.ATTRIBUTE_CONNECTION,
								con));
	}
	
	private void parseCommand(String cmd, String[] cmdParts) throws IOException {
		int l;
		int r;
		l = cmd.indexOf(' ');
		r = cmd.indexOf(' ', l + 1);

		if ((l == -1) || (r == -1)) {
			writeError("Syntax error in cmd");
			throw new IOException("Syntax error in cmd");
		}

		cmdParts[0] = cmd.substring(1, l);
		cmdParts[1] = cmd.substring(l + 1, r);
		cmdParts[2] = cmd.substring(r + 1);
	}

	private String readString() throws IOException {
		int ch;
		int i = 0;

		while (((ch = getInputStream().read()) != ('\n')) && (ch >= 0)) {
			buffer[i++] = (byte) ch;
		}

		if (ch == -1) {
			throw new EOFException("SCP returned unexpected EOF");
		}

		if (buffer[0] == (byte) '\n') {
			throw new IOException("Unexpected <NL>");
		}

		if ((buffer[0] == (byte) '\02') || (buffer[0] == (byte) '\01')) {
			String msg = new String(buffer, 1, i - 1);

			if (buffer[0] == (byte) '\02') {
				throw new IOException(msg);
			}

			throw new IOException("SCP returned an unexpected error: " + msg);
		}

		return new String(buffer, 0, i);
	}
}
