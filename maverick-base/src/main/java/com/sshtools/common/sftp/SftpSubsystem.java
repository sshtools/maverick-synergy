package com.sshtools.common.sftp;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.FileExistsException;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.ExecutorOperationQueues;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SessionChannelHelper;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;
import com.sshtools.common.util.Utils;
import com.sshtools.common.util.Version;

/**
 * This class provides the SFTP subsystem. The subsystem obtains an instance of
 * the configured {@link com.sshtools.common.sftp.FileSystem} to serve files
 * through the SFTP protocol. This implementation currently supports up to
 * version 3 of the protocol.
 * 
 * @author Lee David Painter
 */
public class SftpSubsystem extends Subsystem implements SftpSpecification {
	
	public static final Integer SFTP_QUEUE = ExecutorOperationQueues.generateUniqueQueue("Subsystem.queue");
	
	private AbstractFileSystem nfs;
	private List<SftpOperationWrapper> wrappers = new ArrayList<SftpOperationWrapper>();
	private SshConnection con;
	private boolean nfsClosed = false;
	
	// maximum version of SFTP protocol supported
	static final int MAX_VERSION = 4;
	
	public static final String SUBSYSTEM_NAME = "sftp";
	
	int version;

	private String CHARSET_ENCODING;
	private FileSystemPolicy filePolicy = new FileSystemPolicy();
	private Map<String, TransferEvent> openFileHandles = new ConcurrentHashMap<String, TransferEvent>(8, 0.9f, 1);
	private Map<String, TransferEvent> openFolderHandles = new ConcurrentHashMap<String, TransferEvent>(8, 0.9f, 1);
	private Map<Context, Set<String>> openFilesByContext = new ConcurrentHashMap<Context, Set<String>>(8, 0.9f, 1);

	
	public SftpSubsystem() {
		super("sftp");
	}

	public void init(SessionChannel session, Context context)
			throws IOException, PermissionDeniedException {

		super.init(session, context);
		
		this.filePolicy = context.getPolicy(FileSystemPolicy.class);
		this.con = session.getConnection();
		
		// Check charset encoding
		try {
			"1234567890".getBytes(filePolicy.getSFTPCharsetEncoding());
			this.CHARSET_ENCODING = filePolicy.getSFTPCharsetEncoding();
		} catch (UnsupportedEncodingException ex) {
			if(Log.isDebugEnabled())
				Log.debug(filePolicy.getSFTPCharsetEncoding()
						+ " is not a supported character set encoding. Defaulting to ISO-8859-1");
			CHARSET_ENCODING = "ISO-8859-1";
		}

		try {
			AbstractFileFactory<?> ff = filePolicy.getFileFactory().getFileFactory(session.getConnection());
			
			if(filePolicy.getFileFactory() instanceof SftpOperationWrapper) {
				addWrapper((SftpOperationWrapper)ff);
			}
			
			executeOperation(SUBSYSTEM_INCOMING, new InitOperation());
	
			// Add event listener
			session.addEventListener(new ChannelEventListener() {
				public void onChannelClosing(Channel channel) {
					SessionChannelHelper.sendExitStatus(channel, 0);
				}
			});
		} catch(Throwable t) { 
			throw new PermissionDeniedException(t.getMessage(), t);
		}
	}
	
	protected void cleanupSubsystem() {

		if (!nfsClosed) {

			if(Log.isDebugEnabled()) {
				Log.debug("Cleaning up SFTP subsystem");
			}
			
			long started = System.currentTimeMillis();
			
			List<byte[]> fileHandles = new ArrayList<byte[]>();
			List<byte[]> dirHandles = new ArrayList<byte[]>();
			for(TransferEvent evt : openFileHandles.values()) {
				fileHandles.add(evt.handle);
			}
			for(TransferEvent evt : openFolderHandles.values()) {
				dirHandles.add(evt.handle);
			}
			
			fireEvent(new Event(SftpSubsystem.this,
					EventCodes.EVENT_SFTP_SESSION_STOPPING,
					true)
					.addAttribute(EventCodes.ATTRIBUTE_OPEN_FILE_HANDLES, fileHandles)
					.addAttribute(EventCodes.ATTRIBUTE_OPEN_DIRECTORY_HANDLES, dirHandles)
					.addAttribute(
							EventCodes.ATTRIBUTE_CONNECTION,
							con));
			
			cleanupOpenFiles();
			
			if(nfs!=null) {
				nfs.closeFilesystem();
			}
			
			nfsClosed = true;
			fireEvent(new Event(SftpSubsystem.this,
									EventCodes.EVENT_SFTP_SESSION_STOPPED,
									true)
					.addAttribute(EventCodes.ATTRIBUTE_OPERATION_STARTED, started)
					.addAttribute(EventCodes.ATTRIBUTE_OPERATION_FINISHED, System.currentTimeMillis())
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											con));
		}
	}

	/**
	 * Called to free the subsystem and its resources.
	 */
	protected void onSubsystemFree() {

	}

	class InitOperation extends FileSystemOperation {

		InitOperation() {
			super(null);
		}

		public void doOperation() {

			try {
				nfs = new AbstractFileSystem(
								con,
								AbstractFileSystem.SFTP);

				
				fireEvent(
						new Event(SftpSubsystem.this, EventCodes.EVENT_SFTP_SESSION_STARTED,
								true).addAttribute(
								EventCodes.ATTRIBUTE_CONNECTION,
								con));
				
			} catch (Throwable t) {

				try {
					if(Log.isDebugEnabled())
						Log.debug("An SFTP initialization error occurred", t);
					session.close();
				} catch (Throwable t2) {
				}
			}

		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.INIT;
		}
	}

	protected void onMessageReceived(byte[] msg) throws IOException {

		switch (msg[0] & 0xFF) {
		case SSH_FXP_INIT: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_INIT");
			onInitialize(msg);
			break;
		}

		case SSH_FXP_MKDIR: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_MKDIR");
			executeOperation(SFTP_QUEUE, new MakeDirectoryOperation(msg));
			break;
		}

		case SSH_FXP_REALPATH: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_REALPATH");
			executeOperation(SFTP_QUEUE, new RealPathOperation(msg));
			break;
		}

		case SSH_FXP_OPENDIR: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_OPENDIR");
			executeOperation(SFTP_QUEUE, new OpenDirectoryOperation(msg));
			break;
		}

		case SSH_FXP_OPEN: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_OPEN");
			executeOperation(SFTP_QUEUE, new OpenFileOperation(msg));
			break;
		}

		case SSH_FXP_READ: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_READ");
			executeOperation(SFTP_QUEUE, new ReadFileOperation(msg));
			break;
		}

		case SSH_FXP_WRITE: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_WRITE");
			executeOperation(SFTP_QUEUE, new WriteFileOperation(msg));
			break;
		}

		case SSH_FXP_READDIR: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_READDIR");
			executeOperation(SFTP_QUEUE, new ReadDirectoryOperation(msg));
			break;
		}

		case SSH_FXP_LSTAT: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_LSTAT");
			executeOperation(SFTP_QUEUE, new LStatOperation(msg));
			break;
		}

		case SSH_FXP_STAT: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_STAT");
			executeOperation(SFTP_QUEUE, new StatOperation(msg));
			break;
		}

		case SSH_FXP_FSTAT: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_FSTAT");
			executeOperation(SFTP_QUEUE, new FStatOperation(msg));
			break;
		}

		case SSH_FXP_CLOSE: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_CLOSE");
			executeOperation(SFTP_QUEUE, new CloseFileOperation(msg));
			break;
		}

		case SSH_FXP_REMOVE: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_REMOVE");
			executeOperation(SFTP_QUEUE, new RemoveFileOperation(msg));
			break;
		}

		case SSH_FXP_RENAME: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_RENAME");
			executeOperation(SFTP_QUEUE, new RenameFileOperation(msg));
			break;
		}

		case SSH_FXP_RMDIR: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_RMDIR");
			executeOperation(SFTP_QUEUE, new RemoveDirectoryOperation(msg));
			break;
		}

		case SSH_FXP_SETSTAT: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_SETSTAT");
			executeOperation(SFTP_QUEUE, new SetStatOperation(msg));
			break;
		}

		case SSH_FXP_FSETSTAT: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_FSETSTAT");
			executeOperation(SFTP_QUEUE, new SetFStatOperation(msg));
			break;
		}

		case SSH_FXP_READLINK: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_READLINK");
			executeOperation(SFTP_QUEUE, new ReadlinkOperation(msg));
			break;
		}

		case SSH_FXP_SYMLINK: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_SYMLINK");
			executeOperation(SFTP_QUEUE, new SymlinkOperation(msg));
			break;
		}

		case SSH_FXP_EXTENDED: {
			if(Log.isDebugEnabled())
				Log.debug("Processing SSH_FXP_EXTENDED");
			executeOperation(SFTP_QUEUE, new ExtendedOperation(msg));
			break;
		}

		default:
			for(SftpExtensionFactory fact : getContext().getPolicy(FileSystemPolicy.class).getSFTPExtensionFactories()) {
				for(SftpExtension ext : fact.getExtensions()) {
					if(ext.supportsExtendedMessage(msg[0])) {
						executeOperation(SFTP_QUEUE, new ExtendedMessageOperation(msg, ext));
						break;
					}
				}
			}
			// Don't know this one
			if(Log.isDebugEnabled())
				Log.debug("Processing Unsupported Message id=" + msg[0]);
			executeOperation(SFTP_QUEUE, new UnsupportedOperation(msg));
			break;
		}
	}

	class ExtendedMessageOperation extends FileSystemOperation {
		
		SftpExtension ext;
		ExtendedMessageOperation(byte[] msg, SftpExtension ext) {
			super(msg);
			this.ext = ext;
		}

		@Override
		public void doOperation() {
			ext.processExtendedMessage(new ByteArrayReader(msg), SftpSubsystem.this);
		}
		
		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.EXTENDED;
		}
		
	}
	
	class ExtendedOperation extends FileSystemOperation {

		ExtendedOperation(byte[] msg) {
			super(msg);
		}

		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);
			try {
				
				int id = (int) bar.readInt();
				String requestName = bar.readString();
				
				SftpExtension ext = getContext().getPolicy(FileSystemPolicy.class).getSFTPExtension(requestName);
				
				if(ext!=null) {
					ext.processMessage(bar, id, SftpSubsystem.this);
				} else {
					sendStatusMessage(id,
							STATUS_FX_OP_UNSUPPORTED,
							"Extensions not currently supported");
				}
			} catch (IOException ex) {
			} finally {
				bar.close();
			}
		}
		
		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.EXTENDED;
		}
	}

	class SetStatOperation extends FileSystemOperation {

		SetStatOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.SET_ATTRIBUTES;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			int id = -1;

			Date started = new Date();
			String path = null;
			SftpFileAttributes old = null;
			SftpFileAttributes attrs = null;

			try {
				id = (int) bar.readInt();
				path = checkDefaultPath(bar.readString(CHARSET_ENCODING));

				old = nfs.getFileAttributes(path);

				// The next few bytes are file attributes
				attrs = SftpFileAttributesBuilder.of(bar, version, CHARSET_ENCODING).build();
				
				nfs.setFileAttributes(path, attrs);

				try {
					fireSetStatEvent(path, old, attrs, started, null);
					sendStatusMessage(id, STATUS_FX_OK,
							"The attributes were set");
				} catch (SftpStatusEventException ex) {
					fireSetStatEvent(path, old, attrs, started, ex);
					sendStatusMessage(id, ex.getStatus(), ex.getMessage());
				}

			} catch (FileNotFoundException fnfe) {
				fireSetStatEvent(path, old, attrs, started, fnfe);
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, fnfe.getMessage());
			} catch (PermissionDeniedException pde) {
				fireSetStatEvent(path, old, attrs, started, pde);
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						pde.getMessage());
			} catch (IOException ioe) {
				fireSetStatEvent(path, old, attrs, started, ioe);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe.getMessage());
			}
		}
	}

	protected void fireSetStatEvent(String path, SftpFileAttributes old,
			SftpFileAttributes attrs, Date started, Exception error) {

		fireEvent(
						new Event(SftpSubsystem.this,
								EventCodes.EVENT_SFTP_SET_ATTRIBUTES, error)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										con)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										path)
								.addAttribute(
										EventCodes.ATTRIBUTE_OLD_ATTRIBUTES,
										old)
								.addAttribute(
										EventCodes.ATTRIBUTE_NEW_ATRTIBUTES,
										attrs)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));
	}
	
	protected void fireStatEvent(String path,
			SftpFileAttributes attrs, Date started, Exception error) {

		fireEvent(
						new Event(SftpSubsystem.this,
								EventCodes.EVENT_SFTP_GET_ATTRIBUTES, error)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										con)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										path)
								.addAttribute(
										EventCodes.ATTRIBUTE_ATRTIBUTES,
										attrs)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));
	}

	class SetFStatOperation extends FileSystemOperation {

		SetFStatOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.SET_ATTRIBUTES;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			int id = -1;

			Date started = new Date();
			String path = null;
			SftpFileAttributes attrs = null;
			SftpFileAttributes old = null;

			try {
				id = (int) bar.readInt();
				byte[] handle = bar.readBinaryString();
				old = nfs.getFileAttributes(handle);
				path = nfs.getPathForHandle(handle);

				// The next few bytes are file attributes
				attrs = SftpFileAttributesBuilder.of(bar, version, CHARSET_ENCODING).build();
				nfs.setFileAttributes(handle, attrs);

				try {
					fireSetStatEvent(path, old, attrs, started, null);
					sendStatusMessage(id, STATUS_FX_OK,
							"The attributes were set");
				} catch (SftpStatusEventException ex) {
					sendStatusMessage(id, ex.getStatus(), ex.getMessage());
				}

			} catch (InvalidHandleException ihe) {
				fireSetStatEvent(path, old, attrs, started, ihe);
				sendStatusMessage(id, STATUS_FX_FAILURE, ihe.getMessage());
			} catch (PermissionDeniedException pde) {
				fireSetStatEvent(path, old, attrs, started, pde);
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						pde.getMessage());
			} catch (IOException ioe) {
				fireSetStatEvent(path, old, attrs, started, ioe);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe.getMessage());
			}
		}

	}

	class ReadlinkOperation extends FileSystemOperation {

		ReadlinkOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.FOLLOW_SYMLINK;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			int id = -1;
			try {
				id = (int) bar.readInt();
				SftpFile[] files = new SftpFile[1]; 
				files[0] = nfs.readSymbolicLink(checkDefaultPath(bar
						.readString(CHARSET_ENCODING)));
				sendFilenameMessage(id, files, false, true);

			} catch (FileNotFoundException ioe) {
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
			} catch (PermissionDeniedException pde) {
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						pde.getMessage());
			} catch (UnsupportedFileOperationException uso) {
				sendStatusMessage(id, STATUS_FX_OP_UNSUPPORTED,
						uso.getMessage());
			} catch (IOException ioe2) {
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} finally {
				bar.close();
			}
		}

	}

	class SymlinkOperation extends FileSystemOperation {

		SymlinkOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.CREATE_SYMLINK;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			int id = -1;

			Date started = new Date();
			String linkpath = null;
			String targetpath = null;

			try {
				id = (int) bar.readInt();
				linkpath = bar.readString(CHARSET_ENCODING);
				targetpath = bar.readString(CHARSET_ENCODING);
				nfs.createSymbolicLink(checkDefaultPath(linkpath),
						checkDefaultPath(targetpath));

				try {
					fireSymlinkEvent(linkpath, targetpath, started, null);
					sendStatusMessage(id, STATUS_FX_OK,
							"The symbolic link was created");
				} catch (SftpStatusEventException ex) {
					sendStatusMessage(id, ex.getStatus(), ex.getMessage());
				}
			} catch (FileNotFoundException ioe) {
				fireSymlinkEvent(linkpath, targetpath, started, ioe);
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
			} catch (PermissionDeniedException pde) {
				fireSymlinkEvent(linkpath, targetpath, started, pde);
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						pde.getMessage());
			} catch (IOException ioe2) {
				fireSymlinkEvent(linkpath, targetpath, started, ioe2);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} catch (UnsupportedFileOperationException uso) {
				fireSymlinkEvent(linkpath, targetpath, started, uso);
				sendStatusMessage(id, STATUS_FX_OP_UNSUPPORTED,
						uso.getMessage());
			} finally {
				bar.close();
			}
		}
	}
	
	protected void fireSymlinkEvent(String linkpath, String targetpath,
			Date started, Exception error) {
		fireEvent(new Event(SftpSubsystem.this,
								EventCodes.EVENT_SFTP_SYMLINK_CREATED,
								error)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										con)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										linkpath)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_TARGET,
										targetpath)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));
	}

	class RemoveDirectoryOperation extends FileSystemOperation {

		RemoveDirectoryOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.REMOVE_DIRECTORY;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);
			String path = null;
			Date started = new Date();

			int id = -1;
			try {
				id = (int) bar.readInt();
				path = checkDefaultPath(bar.readString(CHARSET_ENCODING));
				nfs.removeDirectory(path);

				try {
					fireRmDirEvent(path, started, null);
					sendStatusMessage(id, STATUS_FX_OK,
							"The directory was removed");

				} catch (SftpStatusEventException ex) {
					sendStatusMessage(id, ex.getStatus(), ex.getMessage());
				}

			} catch (FileNotFoundException ioe) {
				fireRmDirEvent(path, started, ioe);
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
			} catch (IOException ioe2) {
				fireRmDirEvent(path, started, ioe2);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} catch (PermissionDeniedException pde) {
				fireRmDirEvent(path, started, pde);
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						pde.getMessage());
			} finally {
				bar.close();
			}
		}
	}

	protected void fireRmDirEvent(String path, Date started, Exception error) {
		fireEvent(new Event(SftpSubsystem.this,
								EventCodes.EVENT_SFTP_DIRECTORY_DELETED,
								error)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										con)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										path)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));
	}

	class RenameFileOperation extends FileSystemOperation {

		RenameFileOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.RENAME_FILE;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);
			Date started = new Date();
			String oldpath = null;
			String newpath = null;

			int id = -1;
			try {
				id = (int) bar.readInt();
				oldpath = bar.readString(CHARSET_ENCODING);
				newpath = bar.readString(CHARSET_ENCODING);
				nfs.renameFile(checkDefaultPath(oldpath),
						checkDefaultPath(newpath));

				try {
					fireRenameFileEvent(oldpath, newpath, started, null);
					sendStatusMessage(id, STATUS_FX_OK, "The file was renamed");
				} catch (SftpStatusEventException ex) {
					sendStatusMessage(id, ex.getStatus(), ex.getMessage());
				}
			} catch (FileNotFoundException ioe) {
				fireRenameFileEvent(oldpath, newpath, started, ioe);
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
			} catch (IOException ioe2) {
				fireRenameFileEvent(oldpath, newpath, started, ioe2);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} catch (PermissionDeniedException pde) {
				fireRenameFileEvent(oldpath, newpath, started, pde);
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						pde.getMessage());
			} finally {
				bar.close();
			}
		}
	}

	protected void fireRenameFileEvent(String oldpath, String newpath,
			Date started, Exception error) {
		fireEvent(new Event(SftpSubsystem.this,
								EventCodes.EVENT_SFTP_FILE_RENAMED, error)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										con)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										oldpath)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NEW_NAME,
										newpath)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));
	}

	class RemoveFileOperation extends FileSystemOperation {

		RemoveFileOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.REMOVE_FILE;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);
			String path = null;
			Date started = new Date();

			int id = -1;
			try {
				id = (int) bar.readInt();

				path = checkDefaultPath(bar.readString(CHARSET_ENCODING));
				nfs.removeFile(path);

				try {
					fireRemoveFileEvent(path, started, null);
					sendStatusMessage(id, STATUS_FX_OK, "The file was removed");
				} catch (SftpStatusEventException ex) {
					sendStatusMessage(id, ex.getStatus(), ex.getMessage());
				}
			} catch (FileIsDirectoryException fed) {
				fireRemoveFileEvent(path, started, fed);
				sendStatusMessage(id, SSH_FX_FILE_IS_A_DIRECTORY, fed.getMessage());
			} catch (FileNotFoundException ioe) {
				fireRemoveFileEvent(path, started, ioe);
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
			} catch (IOException ioe2) {
				fireRemoveFileEvent(path, started, ioe2);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} catch (PermissionDeniedException pde) {
				fireRemoveFileEvent(path, started, pde);
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						pde.getMessage());
			} finally {
				bar.close();
			}
		}
	}

	protected void fireRemoveFileEvent(String path, Date started,
			Exception error) {
		fireEvent(	new Event(SftpSubsystem.this,
								EventCodes.EVENT_SFTP_FILE_DELETED, error)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										con)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										path)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));
	}

	class OpenFileOperation extends FileSystemOperation {

		OpenFileOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.OPEN_FILE;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			int id = -1;

			String path = "";
			UnsignedInteger32 flags = new UnsignedInteger32(0);
			Optional<UnsignedInteger32> accessFlags = Optional.empty();
			Date started = new Date();
			SftpFileAttributes attrs = null;

			
			try {
				id = (int) bar.readInt();
				path = checkDefaultPath(bar.readString(CHARSET_ENCODING));
				if (version > 4) {
					accessFlags = Optional.of(new UnsignedInteger32(bar.readInt())); 
				}
				flags = new UnsignedInteger32(bar.readInt());
				attrs = SftpFileAttributesBuilder.of(bar, version, CHARSET_ENCODING).build();
				
				if(getContext().getPolicy(FileSystemPolicy.class).getMaxConcurrentTransfers() > -1 && openFilesByContext.containsKey(getContext())) {
					
					Set<String> openHandles = openFilesByContext.get(getContext());
					if(openHandles.size() >= getContext().getPolicy(FileSystemPolicy.class).getMaxConcurrentTransfers()) {
						fireOpenInitEvent(flags, attrs, path, started, new PermissionDeniedException("Maximum concurrent transfers exceeded for the current context"));
						sendStatusMessage(id, SftpStatusEventException.SSH_FX_PERMISSION_DENIED, "Maximum concurrent transfers exceeded for the current context");
						return;
					}
				}
				
				fireOpenInitEvent(flags, attrs, path, started, null);
				
				boolean exists = false;

				try {
					exists = nfs.fileExists(path);
				} catch (IOException ex) {
				}

				byte[] handle = nfs.openFile(path, flags, accessFlags, attrs);

				TransferEvent evt = new TransferEvent();
				evt.path = path;
				evt.nfs = nfs;
				evt.handle = handle;
				evt.exists = exists;
				evt.flags = flags;
				evt.key = new String(handle);

				try {
					fireOpenFileEvent(flags, attrs, path, started, handle, null);
					
					addTransferEvent(path, evt);

 					sendHandleMessage(id, handle);	
				} catch (SftpStatusEventException ex) {
					sendStatusMessage(id, ex.getStatus(), ex.getMessage());
					try {
						nfs.closeFile(handle);
					} catch (InvalidHandleException e) {
					} finally {
						nfs.freeHandle(handle);
					}
				}

				return;

			} catch (NoSuchFileException | FileNotFoundException ioe) {
				fireOpenFileEvent(flags, attrs, path, started, null, ioe);
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
			} catch (IOException ioe2) {
				fireOpenFileEvent(flags, attrs, path, started, null, ioe2);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} catch (PermissionDeniedException pde) {
				fireOpenFileEvent(flags, attrs, path, started, null, pde);
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						pde.getMessage());
			}

		}
	}

	protected void fireOpenFileEvent(UnsignedInteger32 flags,
			SftpFileAttributes attrs, String path, Date started, byte[] handle, Exception error) {
		if ((flags.longValue() & AbstractFileSystem.OPEN_READ) != AbstractFileSystem.OPEN_READ
				&& ((flags.longValue() & AbstractFileSystem.OPEN_WRITE) == AbstractFileSystem.OPEN_WRITE || (flags
						.longValue() & AbstractFileSystem.OPEN_APPEND) == AbstractFileSystem.OPEN_APPEND)) {

			fireEvent(	new Event(
									SftpSubsystem.this,
									EventCodes.EVENT_SFTP_FILE_UPLOAD_STARTED,
									error)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											con)
									.addAttribute(
											EventCodes.ATTRIBUTE_NEW_ATRTIBUTES,
											attrs)
									.addAttribute(
											EventCodes.ATTRIBUTE_HANDLE,
											handle)
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_NAME,
											path)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_STARTED,
											started)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_FINISHED,
											new Date()));

			// So does this
		} else if ((flags.longValue() & AbstractFileSystem.OPEN_READ) == AbstractFileSystem.OPEN_READ
				&& ((flags.longValue() & AbstractFileSystem.OPEN_WRITE) != AbstractFileSystem.OPEN_WRITE && (flags
						.longValue() & AbstractFileSystem.OPEN_APPEND) != AbstractFileSystem.OPEN_APPEND)) {

			fireEvent(	new Event(
									SftpSubsystem.this,
									EventCodes.EVENT_SFTP_FILE_DOWNLOAD_STARTED,
									error)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											con)
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_NAME,
											path)
									.addAttribute(
											EventCodes.ATTRIBUTE_HANDLE,
											handle)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_STARTED,
											started)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_FINISHED,
											new Date()));

		} else {

			fireEvent(		new Event(SftpSubsystem.this,
									EventCodes.EVENT_SFTP_FILE_ACCESS_STARTED,
									error)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											con)
									.addAttribute(
											EventCodes.ATTRIBUTE_HANDLE,
											handle)
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_NAME,
											path)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_STARTED,
											started)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_FINISHED,
											new Date()));
		}
	}
	
	protected void fireOpenInitEvent(UnsignedInteger32 flags,
			SftpFileAttributes attrs, String path, Date started, Exception error) {
		if ((flags.longValue() & AbstractFileSystem.OPEN_READ) != AbstractFileSystem.OPEN_READ
				&& ((flags.longValue() & AbstractFileSystem.OPEN_WRITE) == AbstractFileSystem.OPEN_WRITE || (flags
						.longValue() & AbstractFileSystem.OPEN_APPEND) == AbstractFileSystem.OPEN_APPEND)) {

			fireEvent(	new Event(
									SftpSubsystem.this,
									EventCodes.EVENT_SFTP_FILE_UPLOAD_INIT,
									error)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											con)
									.addAttribute(
											EventCodes.ATTRIBUTE_NEW_ATRTIBUTES,
											attrs)
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_NAME,
											path)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_STARTED,
											started)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_FINISHED,
											new Date()));

			// So does this
		} else if ((flags.longValue() & AbstractFileSystem.OPEN_READ) == AbstractFileSystem.OPEN_READ
				&& ((flags.longValue() & AbstractFileSystem.OPEN_WRITE) != AbstractFileSystem.OPEN_WRITE && (flags
						.longValue() & AbstractFileSystem.OPEN_APPEND) != AbstractFileSystem.OPEN_APPEND)) {

			fireEvent(	new Event(
									SftpSubsystem.this,
									EventCodes.EVENT_SFTP_FILE_DOWNLOAD_INIT,
									error)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											con)
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_NAME,
											path)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_STARTED,
											started)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_FINISHED,
											new Date()));

		} else {

			fireEvent(		new Event(SftpSubsystem.this,
									EventCodes.EVENT_SFTP_FILE_ACCESS_INIT,
									error)
									.addAttribute(
											EventCodes.ATTRIBUTE_CONNECTION,
											con)
									.addAttribute(
											EventCodes.ATTRIBUTE_FILE_NAME,
											path)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_STARTED,
											started)
									.addAttribute(
											EventCodes.ATTRIBUTE_OPERATION_FINISHED,
											new Date()));
		}
	}

	public void sendHandleMessage(int id, byte[] handle) throws IOException {
		Packet reply = new Packet(handle.length + 9);
		reply.write(SSH_FXP_HANDLE);
		reply.writeInt(id);
		reply.writeBinaryString(handle);

		sendMessage(reply);
	}

	class ReadFileOperation extends FileSystemOperation {
		ReadFileOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.READ_FILE;
		}
		
		public void doOperation() {

			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			int id = -1;
			TransferEvent evt = null;
			Date started = new Date();

			try {

				// Extract the read request from the message
				id = (int) bar.readInt();
				byte[] handle = bar.readBinaryString();
				String h = new String(handle);

				evt = (TransferEvent) openFileHandles.get(h);

				UnsignedInteger64 offset = bar.readUINT64();
				int count = (int) bar.readInt();

				// Construct the correct size packet and read the file
				Packet reply = new Packet(count + 13);
				try {
					reply.write(SSH_FXP_DATA);
					reply.writeInt(id);
	
					// Save the current position so we can update the length later
					int position = reply.position();
					reply.writeInt(0);
	
					if(Log.isDebugEnabled())
						Log.debug("Remote client wants " + String.valueOf(count)
								+ " bytes from file at offset " + offset.toString()
								+ " localwindow=" + session.getLocalWindow()
								+ " remotewindow=" + session.getRemoteWindow());
	
					// Read from the file
					count = nfs.readFile(handle, offset, reply.array(),
							reply.position(), count);
	
					if (count == -1) {
						if (Log.isDebugEnabled()) {
							Log.debug("Got EOF from filesystem");
						}
						evt.hasReachedEOF = true;
						sendStatusMessage(id, STATUS_FX_EOF, "File is EOF");
						return;
					} else {
						evt.bytesRead += count;
	
						if(Log.isDebugEnabled())
							Log.debug("Read " + count + " bytes from filesystem");
	
						// Update the position and write the correct length
						position = reply.setPosition(position);
						reply.writeInt(count);
						reply.setPosition(position + count);
	
						try {
							if(context.getPolicy(FileSystemPolicy.class).isSFTPReadWriteEvents()) {
								fireEvent(new Event(
										SftpSubsystem.this,
										EventCodes.EVENT_SFTP_FILE_READ,
										!evt.error)
										.addAttribute(
												EventCodes.ATTRIBUTE_CONNECTION,
												con)
										.addAttribute(
												EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
												Long.valueOf(evt.bytesRead))
										.addAttribute(
												EventCodes.ATTRIBUTE_BYTES_READ,
												Long.valueOf(count))
										.addAttribute(
												EventCodes.ATTRIBUTE_FILE_NAME,
												evt.path)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_STARTED,
												started)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_FINISHED,
												new Date()));
							}
							sendMessage(reply);
							
						} catch(SftpStatusEventException ex) {
							sendStatusMessage(id, ex.getStatus(), ex.getMessage());
						}
						
					}
				} finally {
					try {
						reply.close();
					} catch (IOException e) {
					}
				}
				
				
				return;
			} catch (EOFException eof) {
				sendStatusMessage(id, STATUS_FX_EOF, eof.getMessage());
			} catch(PermissionDeniedException e) {
				if (evt != null) {
					evt.error = true;
					evt.ex = e;
				}
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED, e.getMessage());
		    } catch (InvalidHandleException ihe) {
				if (evt != null) {
					evt.error = true;
					evt.ex = ihe;
				}
				sendStatusMessage(id, STATUS_FX_FAILURE, ihe.getMessage());
			} catch (FileNotFoundException ioe2) {
				if (evt != null) {
					evt.error = true;
					evt.ex = ioe2;
				}
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe2.getMessage());
			} catch (IOException ioe2) {
				if (evt != null) {
					evt.error = true;
					evt.ex = ioe2;
				}
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} finally {
				bar.close();
			}
			
			if(evt!=null && evt.error && context.getPolicy(FileSystemPolicy.class).isSFTPReadWriteEvents()) {
				fireEvent(	new Event(
						SftpSubsystem.this,
						EventCodes.EVENT_SFTP_FILE_READ,
						!evt.error)
						.addAttribute(
								EventCodes.ATTRIBUTE_CONNECTION,
								con)
						.addAttribute(
								EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
								Long.valueOf(evt.bytesRead))
						.addAttribute(
								EventCodes.ATTRIBUTE_FILE_NAME,
								evt.path)
						.addAttribute(
								EventCodes.ATTRIBUTE_OPERATION_STARTED,
								started)
						.addAttribute(
								EventCodes.ATTRIBUTE_OPERATION_FINISHED,
								new Date())
						.addAttribute(
								EventCodes.ATTRIBUTE_THROWABLE,
								evt.ex));
			}

		}
	}

	class WriteFileOperation extends FileSystemOperation {
		WriteFileOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.WRITE_FILE;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			int id = -1;
			TransferEvent evt = null;
			Date started = new Date();
			
			try {
				id = (int) bar.readInt();
				byte[] handle = bar.readBinaryString();

				String h = new String(handle);

				evt = (TransferEvent) openFileHandles.get(h);
				
				UnsignedInteger64 offset = bar.readUINT64();
				int count = (int) bar.readInt();

				if(filePolicy.hasUploadQuota()) {
					if(!con.containsProperty("uploadQuota")) {
						con.setProperty("uploadQuota", Long.valueOf(0L));
					}
					Long quota = (Long) con.getProperty("uploadQuota");
					if(quota + count > filePolicy.getConnectionUploadQuota()) {
						sendStatusMessage(id, SSH_FX_QUOTA_EXCEEDED, "User upload quota exceeded");
						return;
					}
					
					con.setProperty("uploadQuota", Long.valueOf(quota + count));
				}
				try {	
					
					nfs.writeFile(handle, offset, bar.array(), bar.getPosition(),
							count);
	
					evt.bytesWritten += count;


					if(context.getPolicy(FileSystemPolicy.class).isSFTPReadWriteEvents()) {
						fireEvent(	new Event(
								SftpSubsystem.this,
								EventCodes.EVENT_SFTP_FILE_WRITE,
								!evt.error)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										con)
								.addAttribute(
										EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
										Long.valueOf(evt.bytesWritten))
								.addAttribute(
										EventCodes.ATTRIBUTE_BYTES_WRITTEN,
										Long.valueOf(count))
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										evt.path)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));
					}
					sendStatusMessage(id, STATUS_FX_OK,
							"The write completed successfully");
				} catch(SftpStatusEventException ex) {
					sendStatusMessage(id, ex.getStatus(), ex.getMessage());
				}
				
				return;
			} catch (InvalidHandleException ihe) {
				if (evt != null) {
					evt.error = true;
					evt.ex = ihe;
				}
				sendStatusMessage(id, STATUS_FX_FAILURE, ihe.getMessage());
			}  catch(PermissionDeniedException e) {
				if (evt != null) {
					evt.error = true;
					evt.ex = e;
				}
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED, e.getMessage());
		    } catch (FileNotFoundException ioe2) {
				if (evt != null) {
					evt.error = true;
					evt.ex = ioe2;
				}
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe2.getMessage());
			} catch (IOException ioe2) {
				if (evt != null) {
					evt.error = true;
					evt.ex = ioe2;
				}
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} finally {
				bar.close();
			}
			
			if(evt!=null && evt.error && context.getPolicy(FileSystemPolicy.class).isSFTPReadWriteEvents()) {
				fireEvent(	new Event(
						SftpSubsystem.this,
						EventCodes.EVENT_SFTP_FILE_WRITE,
						!evt.error)
						.addAttribute(
								EventCodes.ATTRIBUTE_CONNECTION,
								con)
						.addAttribute(
								EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
								Long.valueOf(evt.bytesWritten))
						.addAttribute(
								EventCodes.ATTRIBUTE_FILE_NAME,
								evt.path)
						.addAttribute(
								EventCodes.ATTRIBUTE_OPERATION_STARTED,
								started)
						.addAttribute(
								EventCodes.ATTRIBUTE_OPERATION_FINISHED,
								new Date())
						.addAttribute(
								EventCodes.ATTRIBUTE_THROWABLE,
								evt.ex));
			}
		}
	}

	class UnsupportedOperation extends FileSystemOperation {
		UnsupportedOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.UNSUPPORTED;
		}
		
		public void doOperation() {
			
			if(Log.isDebugEnabled()) {
				Log.debug("Unsupported SFTP message received [id="
						+ msg[0] + "]");
			}
			
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			
			try {
				// skip the messagetype byte
				bar.skip(1);
	
				int id = -1;

				try {
					id = (int) bar.readInt();
					sendStatusMessage(id, STATUS_FX_OP_UNSUPPORTED, "Unexpected message id " + msg[0]);
				
				} catch(IOException e) {
					Log.error("Failed to read message id", e);
					con.disconnect("I/O error during read operation");
				}
			} finally {
				bar.close();
			}
		}
	}
	class CloseFileOperation extends FileSystemOperation {

		CloseFileOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.CLOSE_HANDLE;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			int id = -1;
			byte[] handle = null;
			try {
				id = (int) bar.readInt();

				handle = bar.readBinaryString();

				nfs.closeFile(handle);

				try {
					fireCloseFileEvent(handle, null);
					sendStatusMessage(id, STATUS_FX_OK,
							"The operation completed");
				} catch (SftpStatusEventException ex) {
					sendStatusMessage(id, ex.getStatus(), ex.getMessage());
				} 
			} catch (InvalidHandleException ihe) {
				fireCloseFileEvent(handle, ihe);
				sendStatusMessage(id, STATUS_FX_FAILURE, ihe.getMessage());
			} catch (IOException ioe2) {
				fireCloseFileEvent(handle, ioe2);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} finally {
				nfs.freeHandle(handle);
				bar.close();
			}
		}

	}

	protected void fireCloseFileEvent(byte[] handle, Exception error) {

		String key = new String(handle);
		if(openFileHandles.containsKey(key)) {
			TransferEvent evt = (TransferEvent) openFileHandles.remove(key);
			fireCloseFileEvent(evt, error);
			openFileHandles.remove(key);
			openFilesByContext.get(getContext()).remove(key);
			
			if(Log.isDebugEnabled()) {
				Log.debug("There are now {} file(s) open in the current context", 
						openFilesByContext.get(getContext()).size());
			}
		} else if(openFolderHandles.containsKey(key)) {
			TransferEvent evt = (TransferEvent) openFolderHandles.remove(key);
			fireCloseFileEvent(evt, error);
			openFolderHandles.remove(key);
		}
		
	}

	protected void fireCloseFileEvent(TransferEvent evt, Exception error) {
		if (evt != null) {

			if (!evt.error && error != null) {
				evt.error = true;
			}

			boolean closed = false;
			
			if(evt.error && getContext().getPolicy(FileSystemPolicy.class).isSFTPCloseFileBeforeFailedTransferEvents()) {
				try {
					nfs.closeFile(evt.handle);
				} catch (InvalidHandleException e) {
				} catch (IOException e) {
				} finally {
					nfs.freeHandle(evt.handle);
				}
				closed = true;
			}
			
			if (evt.isDir) {

				fireEvent(	new Event(SftpSubsystem.this,
										EventCodes.EVENT_SFTP_DIR,
										error)
										.addAttribute(
												EventCodes.ATTRIBUTE_CONNECTION,
												con)
										.addAttribute(
												EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
												Long.valueOf(evt.bytesWritten))
										.addAttribute(
												EventCodes.ATTRIBUTE_FILE_NAME,
												evt.path)
										.addAttribute(
												EventCodes.ATTRIBUTE_HANDLE,
												evt.handle)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_STARTED,
												evt.started)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_FINISHED,
												new Date()));

			} else if (evt.bytesWritten > 0 && evt.bytesRead <= 0) {
				fireEvent(	new Event(
										SftpSubsystem.this,
										EventCodes.EVENT_SFTP_FILE_UPLOAD_COMPLETE,
										error)
										.addAttribute(
												EventCodes.ATTRIBUTE_CONNECTION,
												con)
										.addAttribute(
												EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
												Long.valueOf(evt.bytesWritten))
										.addAttribute(
												EventCodes.ATTRIBUTE_FILE_NAME,
												evt.path)
										.addAttribute(
												EventCodes.ATTRIBUTE_HANDLE,
												evt.handle)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_STARTED,
												evt.started)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_FINISHED,
												new Date()));
			} else if (evt.bytesRead > 0 && evt.bytesWritten <= 0) {

				fireEvent(	new Event(
										SftpSubsystem.this,
										EventCodes.EVENT_SFTP_FILE_DOWNLOAD_COMPLETE,
										error)
										.addAttribute(
												EventCodes.ATTRIBUTE_CONNECTION,
												con)
										.addAttribute(
												EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
												Long.valueOf(evt.bytesRead))
										.addAttribute(
												EventCodes.ATTRIBUTE_HANDLE,
												evt.handle)
										.addAttribute(
												EventCodes.ATTRIBUTE_FILE_NAME,
												evt.path)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_STARTED,
												evt.started)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_FINISHED,
												new Date()));

			} else if (evt.bytesRead <= 0
					&& evt.bytesWritten <= 0
					&& (evt.flags.longValue() & AbstractFileSystem.OPEN_READ) != AbstractFileSystem.OPEN_READ
					&& ((evt.flags.longValue() & AbstractFileSystem.OPEN_WRITE) == AbstractFileSystem.OPEN_WRITE || (evt.flags
							.longValue() & AbstractFileSystem.OPEN_APPEND) == AbstractFileSystem.OPEN_APPEND)) {

				if (context.getPolicy(FileSystemPolicy.class).isAllowZeroLengthFileUpload() || evt.exists) {

					fireEvent(	new Event(
											SftpSubsystem.this,
											EventCodes.EVENT_SFTP_FILE_TOUCHED,
											error)
											.addAttribute(
													EventCodes.ATTRIBUTE_CONNECTION,
													con)
											.addAttribute(
													EventCodes.ATTRIBUTE_BYTES_READ,
													Long.valueOf(evt.bytesRead))
											.addAttribute(
													EventCodes.ATTRIBUTE_BYTES_WRITTEN,
													Long.valueOf(evt.bytesWritten))
											.addAttribute(
													EventCodes.ATTRIBUTE_HANDLE,
													evt.handle)
											.addAttribute(
													EventCodes.ATTRIBUTE_FILE_NAME,
													evt.path)
											.addAttribute(
													EventCodes.ATTRIBUTE_OPERATION_STARTED,
													evt.started)
											.addAttribute(
													EventCodes.ATTRIBUTE_OPERATION_FINISHED,
													new Date()));

				} else {

					try {
						nfs.removeFile(evt.path);
					} catch (Exception e) {
					}

					fireEvent(	new Event(
											SftpSubsystem.this,
											EventCodes.EVENT_SFTP_FILE_TOUCHED,
											error)
											.addAttribute(
													EventCodes.ATTRIBUTE_CONNECTION,
													con)
											.addAttribute(
													EventCodes.ATTRIBUTE_BYTES_READ,
													Long.valueOf(evt.bytesRead))
											.addAttribute(
													EventCodes.ATTRIBUTE_BYTES_WRITTEN,
													Long.valueOf(evt.bytesWritten))
											.addAttribute(
													EventCodes.ATTRIBUTE_HANDLE,
													evt.handle)
											.addAttribute(
													EventCodes.ATTRIBUTE_FILE_NAME,
													evt.path)
											.addAttribute(
													EventCodes.ATTRIBUTE_OPERATION_STARTED,
													evt.started)
											.addAttribute(
													EventCodes.ATTRIBUTE_OPERATION_FINISHED,
													new Date()));

					throw new SftpStatusEventException(
							SftpStatusEventException.SSH_FX_FAILURE,
							"Zero length file is not allowed");
				}

			} else if (evt.bytesRead <= 0
					&& evt.bytesWritten <= 0
					&& (evt.flags.longValue() & AbstractFileSystem.OPEN_READ) == AbstractFileSystem.OPEN_READ
					&& ((evt.flags.longValue() & AbstractFileSystem.OPEN_WRITE) != AbstractFileSystem.OPEN_WRITE && (evt.flags
							.longValue() & AbstractFileSystem.OPEN_APPEND) != AbstractFileSystem.OPEN_APPEND)) {

				fireEvent(	new Event(
										SftpSubsystem.this,
										EventCodes.EVENT_SFTP_FILE_DOWNLOAD_COMPLETE,
										error)
										.addAttribute(
												EventCodes.ATTRIBUTE_CONNECTION,
												con)
										.addAttribute(
												EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
												Long.valueOf(evt.bytesRead))
										.addAttribute(
												EventCodes.ATTRIBUTE_FILE_NAME,
												evt.path)
										.addAttribute(
												EventCodes.ATTRIBUTE_HANDLE,
												evt.handle)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_STARTED,
												evt.started)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_FINISHED,
												new Date()));

			} else {
				// Random access transfer

				fireEvent(	new Event(SftpSubsystem.this,
										EventCodes.EVENT_SFTP_FILE_ACCESS,
										error)
										.addAttribute(
												EventCodes.ATTRIBUTE_CONNECTION,
												con)
										.addAttribute(
												EventCodes.ATTRIBUTE_BYTES_READ,
												Long.valueOf(evt.bytesRead))
										.addAttribute(
												EventCodes.ATTRIBUTE_BYTES_WRITTEN,
												Long.valueOf(evt.bytesWritten))
										.addAttribute(
												EventCodes.ATTRIBUTE_FILE_NAME,
												evt.path)
										.addAttribute(
												EventCodes.ATTRIBUTE_HANDLE,
												evt.handle)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_STARTED,
												evt.started)
										.addAttribute(
												EventCodes.ATTRIBUTE_OPERATION_FINISHED,
												new Date()));
			}
			
			if(evt.error && !closed && evt.forceClose) {
				try {
					nfs.closeFile(evt.handle);
				} catch (InvalidHandleException e) {
				} catch (IOException e) {
				} finally {
					nfs.freeHandle(evt.handle);
				}
			}
		}
	}

	class FStatOperation extends FileSystemOperation {

		FStatOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.GET_ATTRIBUTES;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			int id = -1;

			try {
				id = (int) bar.readInt();
				sendAttributesMessage(id,
						nfs.getFileAttributes(bar.readBinaryString()));
			} catch (InvalidHandleException ihe) {
				sendStatusMessage(id, STATUS_FX_FAILURE, ihe.getMessage());
			} catch (PermissionDeniedException ihe) {
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						ihe.getMessage());
			} catch (IOException ioe2) {
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} finally {
				bar.close();
			}
		}

	}

	class StatOperation extends FileSystemOperation {

		StatOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.GET_ATTRIBUTES;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);
			Date started = new Date();
			int id = -1;
			String path = null;
			try {
				id = (int) bar.readInt();
				path = checkDefaultPath(bar.readString(CHARSET_ENCODING));

				if (nfs.fileExists(path)) {
					SftpFileAttributes attrs = nfs.getFileAttributes(path);
					sendAttributesMessage(id, attrs);
					fireStatEvent(path, attrs, started, null);
				} else {
					fireStatEvent(path, null, started, new FileNotFoundException());
					sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, path
							+ " is not a valid file path");
				}
			} catch (FileNotFoundException ioe) {
				fireStatEvent(path, null, started, ioe);
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
			} catch (PermissionDeniedException ioe) {
				fireStatEvent(path, null, started, ioe);
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						ioe.getMessage());
			} catch (IOException ioe2) {
				fireStatEvent(path, null, started, ioe2);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} finally {
				bar.close();
			}
		}
	}

	class LStatOperation extends FileSystemOperation {

		LStatOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.GET_ATTRIBUTES;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);
			Date started = new Date();
			int id = -1;
			String path = null;
			
			try {
				id = (int) bar.readInt();
				path = checkDefaultPath(bar.readString(CHARSET_ENCODING));

				if (nfs.fileExists(path, false)) {
					SftpFileAttributes attrs = nfs.getFileAttributes(path, false);
					sendAttributesMessage(id, attrs);
					fireStatEvent(path, attrs, started, null);
				} else {
					fireStatEvent(path, null, started, new FileNotFoundException());
					sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, path
							+ " is not a valid file path");
				}
				
				
			} catch (FileNotFoundException ioe) {
				fireStatEvent(path, null, started, ioe);
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
			} catch (PermissionDeniedException ioe) {
				fireStatEvent(path, null, started, ioe);
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						ioe.getMessage());
			} catch (IOException ioe2) {
				fireStatEvent(path, null, started, ioe2);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} finally {
				bar.close();
			}
		}

	}

	public void sendAttributesMessage(int id, SftpFileAttributes attrs)
			throws IOException {
		byte[] encoded = attrs.toByteArray(version);
		Packet msg = new Packet(5 + encoded.length);
		msg.write(SSH_FXP_ATTRS);
		msg.writeInt(id);
		msg.write(encoded);

		sendMessage(msg);
	}

	class ReadDirectoryOperation extends FileSystemOperation {

		ReadDirectoryOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.READ_DIRECTORY;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			int id = -1;
			byte[] handle = null;
			try {
				id = (int) bar.readInt();
				handle = bar.readBinaryString();
				
				TransferEvent evt = (TransferEvent) openFolderHandles.get(nfs.handleToString(handle));
				evt.bytesWritten += sendFilenameMessage(id, nfs.readDirectory(handle), false, false);
				
			} catch (FileNotFoundException ioe) {
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
			} catch (InvalidHandleException ihe) {
				sendStatusMessage(id, STATUS_FX_FAILURE, ihe.getMessage());
			} catch (EOFException eof) {
				sendStatusMessage(id, STATUS_FX_EOF, eof.getMessage());
			} catch (IOException ioe2) {
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} catch (PermissionDeniedException e) {
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED, e.getMessage());
			} finally {
				bar.close();
			}
		}

	}

	class OpenDirectoryOperation extends FileSystemOperation {

		OpenDirectoryOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.OPEN_DIRECTORY;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			Date started = new Date();

			int id = -1;
			String path = null;

			try {
				id = (int) bar.readInt();
				path = checkDefaultPath(bar.readString(CHARSET_ENCODING));

				TransferEvent evt = new TransferEvent();
				evt.nfs = nfs;
				evt.isDir = true;
				evt.started = new Date();
				evt.path = path;

				byte[] handle = nfs.openDirectory(path);
				evt.handle = handle;
				try {
					fireOpenDirectoryEvent(path, started, handle, null);
					openFolderHandles.put(new String(handle), evt);
					sendHandleMessage(id, handle);
				} catch (SftpStatusEventException ex) {
					sendStatusMessage(id, ex.getStatus(), ex.getMessage());
				}
				return;
			} catch (FileNotFoundException ioe) {
				fireOpenDirectoryEvent(path, started, null, ioe);
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
			} catch (IOException ioe2) {
				fireOpenDirectoryEvent(path, started, null, ioe2);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe2.getMessage());
			} catch (PermissionDeniedException pde) {
				fireOpenDirectoryEvent(path, started, null, pde);
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						pde.getMessage());
			} finally {
				bar.close();
			}
		}
	}

	protected void fireOpenDirectoryEvent(String path, Date started, byte[] handle,
			Exception error) {
		fireEvent(new Event(SftpSubsystem.this, EventCodes.EVENT_SFTP_DIRECTORY_OPENED,
								error)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										con)
								.addAttribute(
										EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
										Long.valueOf(0))
								.addAttribute(
										EventCodes.ATTRIBUTE_HANDLE,
										handle)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										path)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));
	}

	public void sendStatusMessage(int id, int reason, String description) {
		if(Log.isDebugEnabled())
			Log.debug("Sending SSH_FXP_STATUS: : " + description + " reason="
					+ reason);
		try {
			Packet baw = new Packet(1024);
			baw.write(SSH_FXP_STATUS);
			baw.writeInt(id);
			baw.writeInt(reason);

			if (version > 2) {
				baw.writeString(description, CHARSET_ENCODING);
				baw.writeString("");
			}

			sendMessage(baw);
		} catch (IOException ex) {
			session.close();
		}
	}

	class RealPathOperation extends FileSystemOperation {

		RealPathOperation(byte[] msg) {
			super(msg);
		}
		
		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.RESOLVE_PATH;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);

			try {

				// skip the messagetype byte
				bar.skip(1);
				int id = (int) bar.readInt();
				String path = bar.readString(CHARSET_ENCODING);
				// path="";
				try {
					String realpath = nfs.getRealPath(checkDefaultPath(path));

					SftpFile file = new SftpFile(realpath,
							nfs.getFileAttributes(realpath));

					sendFilenameMessage(id, new SftpFile[] { file }, true,
							true);
				} catch (FileNotFoundException ex) {
					sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE,
							ex.getMessage());
				} catch (PermissionDeniedException ioe) {
					sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
							ioe.getMessage());
				}

			} catch (IOException ex) {
				session.close();
			} finally {
				bar.close();
			}
		}
	}

	private String formatLongnameInContext(SftpFile file, Locale locale) {
		return formatLongnameInContext(file.getAttributes(), file.getFilename(), locale);
	}

	private String formatLongnameInContext(SftpFileAttributes attrs,
			String filename, Locale locale) {

		// Permissions(10)
		// "   1"
		// UID(8)
		// space(1)
		// GID(8)
		// space(1)
		// size(8)
		// space(1)
		// modtime(12)
		// space(1)
		// filename

		var str = new StringBuffer();
		var permissionsString = attrs.toPermissionsString();
		str.append(Utils.pad(10 - permissionsString.length()) + permissionsString);
		if(attrs.isDirectory()) {
			str.append(" 1 ");
		} else {
			str.append(" 1 ");
		}
		str.append(attrs.uidOr().map(u -> u + Utils.pad(8 - String.valueOf(u).length())).orElse("       0"));
		str.append(" ");
		str.append(attrs.gidOr().map(g -> g + Utils.pad(8 - String.valueOf(g).length())).orElse("       0"));
		str.append(" ");

		str.append(Utils.pad(11 - attrs.size().toString().length())
				+ attrs.size().toString());
		str.append(" ");
		
		String modTime = getModTimeStringInContext(attrs.lastModifiedTime(), locale);
		str.append(Utils.pad(12 - modTime.length()) + modTime);
		str.append(" ");
		str.append(filename);

		return str.toString();
	}

	private String getModTimeStringInContext(FileTime mtime,
			Locale locale) {
		if (mtime == null) {
			return "";
		}

		SimpleDateFormat df;
		long mt = mtime.toMillis();
		long now = System.currentTimeMillis();

		if ((now - mt) > (6 * 30 * 24 * 60 * 60 * 1000L)) {
			df = new SimpleDateFormat(getContext().getPolicy(FileSystemPolicy.class).getSFTPLongnameDateFormat(), locale);
		} else {
			df = new SimpleDateFormat(getContext().getPolicy(FileSystemPolicy.class).getSFTPLongnameDateFormatWithTime(), locale);
		}

		return df.format(new Date(mt));
	}

	

	public int sendFilenameMessage(int id, SftpFile[] files, boolean isRealPath,
			boolean isAbsolute) throws IOException {

		Packet baw = new Packet(16384);
		baw.write(SSH_FXP_NAME);
		baw.writeInt(id);
		baw.writeInt(files.length);

		for (int i = 0; i < files.length; i++) {
			baw.writeString(
					isAbsolute ? files[i].getAbsolutePath() : files[i]
							.getFilename(), CHARSET_ENCODING);
			if(version <= 3) {
				baw.writeString(isRealPath ? files[i].getAbsolutePath()
						: formatLongnameInContext(files[i], con.getLocale()),
						CHARSET_ENCODING);
			}
			baw.write(files[i].getAttributes().toByteArray(version));
		}

		sendMessage(baw);

		return baw.size();
	}

	class MakeDirectoryOperation extends FileSystemOperation {

		MakeDirectoryOperation(byte[] msg) {
			super(msg);
		}

		@Override
		public SftpSubsystemOperation getOp() {
			return SftpSubsystemOperation.MAKE_DIRECTORY;
		}
		
		public void doOperation() {
			// Read 'msg' as a byte stream
			ByteArrayReader bar = new ByteArrayReader(msg);
			// skip the messagetype byte
			bar.skip(1);

			int id = -1;
			Date started = new Date();
			String path = null;
			SftpFileAttributes attrs = null;
			try {
				id = (int) bar.readInt();
				path = checkDefaultPath(bar.readString(CHARSET_ENCODING));
				if(bar.available() > 0) {
					attrs = SftpFileAttributesBuilder.of(bar, version, CHARSET_ENCODING).build();
				}
				
				boolean exists = nfs.fileExists(path);
				if (!exists && nfs.makeDirectory(path, attrs)) {

					try {
						fireMakeDirectoryEvent(path, started, null);

						sendStatusMessage(id, STATUS_FX_OK,
								"The operation completed sucessfully");
					} catch (SftpStatusEventException ex) {
						sendStatusMessage(id, ex.getStatus(), ex.getMessage());
					}
				} else {

					try {
						fireMakeDirectoryEvent(path, started,
								exists ? new FileExistsException() : new IOException("The operation failed."));

						sendStatusMessage(id, exists ? SSH_FX_FILE_ALREADY_EXISTS : STATUS_FX_NO_SUCH_FILE,
								"The operation failed");
					} catch (SftpStatusEventException ex) {
						sendStatusMessage(id, ex.getStatus(), ex.getMessage());
					}
				}

				return;
			} catch (FileExistsException fe) {
				fireMakeDirectoryEvent(path, started, fe);
				sendStatusMessage(id, STATUS_FX_FAILURE, "File already exists");
			} catch (FileNotFoundException ioe) {
				fireMakeDirectoryEvent(path, started, ioe);
				sendStatusMessage(id, STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
			} catch (PermissionDeniedException pde) {
				fireMakeDirectoryEvent(path, started, pde);
				sendStatusMessage(id, STATUS_FX_PERMISSION_DENIED,
						pde.getMessage());
			} catch (IOException ioe) {
				fireMakeDirectoryEvent(path, started, ioe);
				sendStatusMessage(id, STATUS_FX_FAILURE, ioe.getMessage());
			} finally {
				bar.close();
			}
		}
	}

	protected void fireMakeDirectoryEvent(String path, Date started,
			Exception error) {
		fireEvent(new Event(SftpSubsystem.this,
								EventCodes.EVENT_SFTP_DIRECTORY_CREATED,
								error)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										con)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										path)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));
	}

	public String checkDefaultPath(String path) throws IOException, PermissionDeniedException {
		// Use the users home directory if no path is supplied
		if (path.equals("")) {
			return nfs.getDefaultPath();
		}
		return path;
	}

	private void onInitialize(byte[] msg) throws IOException {
		try {
			int theirVersion = (int) ByteArrayReader.readInt(msg, 1);
			int ourVersion = context.getPolicy(FileSystemPolicy.class).getSFTPVersion();
			version = Math.min(theirVersion, ourVersion);
			Packet packet = new Packet(5);
			packet.write(SSH_FXP_VERSION);
			packet.writeInt(version);
			
			if(Log.isDebugEnabled()) {
				Log.debug("Negotiated SFTP version " + version + " [server=" + ourVersion + " client=" + theirVersion + "]");
			}
			
			if (version > 3) {
				packet.writeString("newline");
				packet.writeString(System.getProperty("line.separator"));
			} else {
				packet.writeString("newline@vandyke.com");
				packet.writeString(System.getProperty("line.separator"));
			}
			
			packet.writeString("vendor-id");
			
			try(ByteArrayWriter writer = new ByteArrayWriter()) {
				writer.writeString("JADAPTIVE Limited");
				writer.writeString("Maverick Synergy");
				writer.writeString(Version.getVersion());
				writer.writeUINT64(new UnsignedInteger64(0));
				packet.writeBinaryString(writer.toByteArray());
			} 
			
			for(SftpExtensionFactory factory : context.getPolicy(FileSystemPolicy.class).getSFTPExtensionFactories()) {
				for(SftpExtension ext : factory.getExtensions()) {
					if(Log.isDebugEnabled()) {
						Log.debug("SFTP supports extension {}", ext.getName());
					}
					if(ext.isDeclaredInVersion()) {
						packet.writeString(ext.getName());
						packet.writeBinaryString(ext.getDefaultData());
					}
				}
			}
			sendMessage(packet);
		} finally {
			onFreeMessage(msg);
		}
	}
	
	public void fireEvent(Event event) {
		if(nfs!=null) {
			nfs.populateEvent(event);
		}
		EventServiceImplementation.getInstance().fireEvent(event);
	}

	abstract class FileSystemOperation extends ConnectionAwareTask {
		protected byte[] msg;

		FileSystemOperation(byte[] msg) {
			super(SftpSubsystem.this.session.getConnection());
			this.msg = msg;
		}
		
		public abstract void doOperation();
		
		public abstract SftpSubsystemOperation getOp();
		
		@Override
		protected void doTask() {
			if(!wrappers.isEmpty()) {
				for(SftpOperationWrapper wrapper : wrappers) {
					try {
						wrapper.onBeginOperation(session, getOp());
					} catch (Throwable e) {
					}
				}
			}
			try {
				doOperation();	
			} finally {
				if(!wrappers.isEmpty()) {
					for(SftpOperationWrapper wrapper : wrappers) {
						try {
							wrapper.onEndOperation(session, getOp());
						} catch (Throwable e) {
						}
					}
				}
				if(msg!=null) {
					onFreeMessage(msg);
				}
				msg = null;
			}
			
		}
	}

	private void cleanupOpenFiles() {

		Iterator<TransferEvent> it = openFileHandles.values().iterator();

		SshException ex = new SshException("The connection has closed", SshException.CONNECTION_CLOSED);
		
		while (it.hasNext()) {
			TransferEvent evt = it.next();
			evt.error = true;
			try {
				fireCloseFileEvent(evt, ex);
			} catch (SftpStatusEventException e) {
			}
			openFilesByContext.get(getContext()).remove(evt.key);
		}
		
		openFileHandles.clear();
		
		it = openFolderHandles.values().iterator();

		while (it.hasNext()) {
			TransferEvent evt = it.next();
			evt.error = true;
			try {
				fireCloseFileEvent(evt, ex);
			} catch (SftpStatusEventException e) {
			}
		}
		openFolderHandles.clear();
	}

	public AbstractFileSystem getFileSystem() {
		return nfs;
	}

	public void submitTask(Runnable runnable) {
		
	}

	public void addWrapper(SftpOperationWrapper wrapper) {
		wrappers.add(wrapper);
	}
	
	public void removeWrapper(SftpOperationWrapper wrapper) {
		wrappers.remove(wrapper);
	}

	public String getCharsetEncoding() {
		return CHARSET_ENCODING;
	}
	
	public void addTransferEvent(String handle, TransferEvent evt) {
		if(evt.isDir()) {
			openFolderHandles.put(evt.key, evt);
		} else {
			openFileHandles.put(evt.key, evt);
		}		
		if(!openFilesByContext.containsKey(getContext())) {
			openFilesByContext.put(getContext(), new HashSet<String>());
		}
		openFilesByContext.get(getContext()).add(evt.key);
		if(Log.isDebugEnabled()) {
			Log.debug("There are now {} file(s) open in the current context", 
					openFilesByContext.get(getContext()).size());
		}
	}

	public int getVersion() {
		return version;
	}

}
