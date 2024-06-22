package com.sshtools.client.sftp;

/*-
 * #%L
 * Client API
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.tasks.AbstractSubsystem;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.client.tasks.Message;
import com.sshtools.client.tasks.MessageHolder;
import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.sftp.ACL;
import com.sshtools.common.sftp.PosixPermissions;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.ssh.RequestFuture;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;
import com.sshtools.synergy.ssh.ByteArrays;
import com.sshtools.synergy.ssh.PacketPool;

/**
 * Abstract task implementing SFTP operations.
 */
public class SftpChannel extends AbstractSubsystem {

	
	private String CHARSET_ENCODING = "UTF-8";
	
	/**
	 * File open flag, opens the file for reading.
	 */
	public static final int OPEN_READ = 0x00000001;

	/**
	 * File open flag, opens the file for writing.
	 */
	public static final int OPEN_WRITE = 0x00000002;

	/**
	 * File open flag, forces all writes to append data at the end of the file.
	 */
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
	 * File open flag, causes the file to be opened in text mode. This instructs
	 * the server to convert the text file to the canonical newline convention
	 * in use. Any files retrieved using this mode should then be converted from
	 * the canonical newline convention to that of the clients.
	 */
	public static final int OPEN_TEXT = 0x00000040;

	static final int STATUS_FX_OK = 0;
	static final int STATUS_FX_EOF = 1;

	static final int SSH_FXP_INIT = 1;
	static final int SSH_FXP_VERSION = 2;
	static final int SSH_FXP_OPEN = 3;
	static final int SSH_FXP_CLOSE = 4;
	static final int SSH_FXP_READ = 5;
	static final int SSH_FXP_WRITE = 6;

	static final int SSH_FXP_LSTAT = 7;
	static final int SSH_FXP_FSTAT = 8;
	static final int SSH_FXP_SETSTAT = 9;
	static final int SSH_FXP_FSETSTAT = 10;
	static final int SSH_FXP_OPENDIR = 11;
	static final int SSH_FXP_READDIR = 12;
	static final int SSH_FXP_REMOVE = 13;
	static final int SSH_FXP_MKDIR = 14;
	static final int SSH_FXP_RMDIR = 15;
	static final int SSH_FXP_REALPATH = 16;
	static final int SSH_FXP_STAT = 17;
	static final int SSH_FXP_RENAME = 18;
	static final int SSH_FXP_READLINK = 19;
	static final int SSH_FXP_SYMLINK = 20;
	static final int SSH_FXP_LINK = 21;
	static final int SSH_FXP_BLOCK = 22;
	static final int SSH_FXP_UNBLOCK = 23;
	
	public static final int SSH_FXP_STATUS = 101;
	public static final int SSH_FXP_HANDLE = 102;
	public static final int SSH_FXP_DATA = 103;
	public static final int SSH_FXP_NAME = 104;
	public static final int SSH_FXP_ATTRS = 105;

	public static final int SSH_FXP_EXTENDED = 200;
	public static final int SSH_FXP_EXTENDED_REPLY = 201;

	public static final int MAX_VERSION = 6;

	Long supportedAttributeMask;
	Long supportedAttributeBits;
	Long supportedOpenFileFlags;
	Long supportedAccessMask;
	short supportedOpenBlockVector;
	short supportedBlockVector;
	
	Integer maxReadSize;
	Set<String> supportedExtensions = new HashSet<String>();
	Set<String> supportedAttrExtensions = new HashSet<String>();
	
	int version = MAX_VERSION;
	int serverVersion = -1;
	UnsignedInteger32 requestId = new UnsignedInteger32(0);
	Map<UnsignedInteger32, SftpMessage> responses = new ConcurrentHashMap<UnsignedInteger32, SftpMessage>();
	SftpThreadSynchronizer sync = new SftpThreadSynchronizer();
	Map<String, byte[]> extensions = new HashMap<String, byte[]>();
	Map<byte[], SftpHandle> handles = Collections.synchronizedMap(new HashMap<byte[], SftpHandle>());

	/**
	 * Version 5 new flags
	 */
	public static final int SSH_FXF_ACCESS_DISPOSITION 			= 0x00000007;
	public static final int SSH_FXF_CREATE_NEW 					= 0x00000000;
	public static final int SSH_FXF_CREATE_TRUNCATE 			= 0x00000001;
	public static final int SSH_FXF_OPEN_EXISTING 				= 0x00000002;
	public static final int SSH_FXF_OPEN_OR_CREATE 				= 0x00000003;
	public static final int SSH_FXF_TRUNCATE_EXISTING 			= 0x00000004;
	public static final int SSH_FXF_ACCESS_APPEND_DATA 			= 0x00000008;
	public static final int SSH_FXF_ACCESS_APPEND_DATA_ATOMIC	= 0x00000010;
	public static final int SSH_FXF_ACCESS_TEXT_MODE 			= 0x00000020;
	public static final int SSH_FXF_ACCESS_BLOCK_READ 			= 0x00000040;
	public static final int SSH_FXF_ACCESS_BLOCK_WRITE 			= 0x00000080;
	public static final int SSH_FXF_ACCESS_BLOCK_DELETE			= 0x00000100;
	public static final int SSH_FXF_ACCESS_BLOCK_ADVISORY		= 0x00000200;
	public static final int SSH_FXF_NOFOLLOW					= 0x00000400;
	public static final int SSH_FXF_DELETE_ON_CLOSE				= 0x00000800;
	public static final int SSH_FXF_ACCESS_AUDIT_ALARM_INFO		= 0x00001000;
	public static final int SSH_FXF_ACCESS_BACKUP				= 0x00002000;
	public static final int SSH_FXF_BACKUP_STREAM				= 0x00004000;
	public static final int SSH_FXF_OVERRIDE_OWNER				= 0x00008000;
	
	
	
	public static final int SSH_FXP_RENAME_OVERWRITE = 0x00000001;
	public static final int SSH_FXP_RENAME_ATOMIC    = 0x00000002;
	public static final int SSH_FXP_RENAME_NATIVE    = 0x00000004;
	
	public SftpChannel(SshConnection con) throws SshException {
		super(con);
		con.setProperty("sftpVersion", initializeSftp(session));
	}

	public int getVersion() {
		return (Integer) con.getProperty("sftpVersion");
	}

	protected UnsignedInteger32 getMinimumWindowSize() {
		return con.getContext().getPolicy(FileSystemPolicy.class).getSftpMinWindowSize();
	}
	
	protected UnsignedInteger32 getMaximumWindowSize() {
		return con.getContext().getPolicy(FileSystemPolicy.class).getSftpMaxWindowSize();
	}
	
	protected int getMaximumPacketSize() {
		return con.getContext().getPolicy(FileSystemPolicy.class).getSftpMaxPacketSize();
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, byte[]> getExtensions() {
		return (Map<String, byte[]>) con.getProperty("sftpExtensions");
	}
	
	@Override
	public SessionChannelNG getSession() {
		return super.getSession();
	}
	
	private int initializeSftp(SessionChannelNG session) throws SshException {

		try {

			RequestFuture future = session.startSubsystem("sftp");
			if(!future.waitFor(timeout).isSuccess()) {
				throw new SshException("Could not start sftp subsystem", SshException.CONNECT_FAILED);
			}
			
			Packet packet = PacketPool.getInstance().getPacket();
			packet.write(SSH_FXP_INIT);
			packet.writeInt(MAX_VERSION);

			sendMessage(packet);

			byte[] msg = nextMessage();

			try {
				if (msg[0] != SSH_FXP_VERSION) {
					session.close();
					throw new SshException(
							"Unexpected response from SFTP subsystem.",
							SshException.CHANNEL_FAILURE);
				}
	
				ByteArrayReader bar = new ByteArrayReader(msg);
	
				try {
					bar.skip(1);
	
					int serverVersion = (int) bar.readInt();
					int requestedVersion = MAX_VERSION;
					version = Math.min(serverVersion, requestedVersion);
	
					if(Log.isTraceEnabled()) {
						Log.trace("Version is " + version + " [Server="
								+ serverVersion + " Client=" + requestedVersion
								+ "]");
					}
					try {
						while (bar.available() > 0) {
							String name = bar.readString();
							byte[] data = bar.readBinaryString();
	
							extensions.put(name, data);
	
							if(Log.isTraceEnabled()) {
								Log.trace("Processed extension '" + name + "'");
							}
						}
					} catch (Throwable t) {
					}
	
					if (version == 5) {
						if (extensions.containsKey("supported")) {
							processSupported(extensions.get("supported"));
						}
					} else if (version >= 6) {
						if (extensions.containsKey("supported2")) {
							processSupported2(extensions.get("supported2"));
						}
					}
					if (version <= 3) {
						setCharsetEncoding("ISO-8859-1");
					} else {
						if (extensions.containsKey("filename-charset")) {
	
							String newCharset = new String(
									extensions.get("filename-charset"), "UTF-8");
							try {
								setCharsetEncoding(newCharset);
								sendExtensionMessage(
										"filename-translation-control",
										new byte[] { 0 });
							} catch (Exception e) {
								setCharsetEncoding("UTF8");
								sendExtensionMessage(
										"filename-translation-control",
										new byte[] { 1 });
							}
						} else {
							setCharsetEncoding("UTF8");
						}
					}
	
					con.setProperty("sftpExtensions", extensions);
	
					return version;
				} finally {
					bar.close();
				}
			} finally {
				ByteArrays.getInstance().releaseByteArray(msg);
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(SshException.CHANNEL_FAILURE, ex);
		} catch (Throwable t) {
			throw new SshException(SshException.CHANNEL_FAILURE, t);
		}
	}
	
	/**
	 * Returns the canonical newline convention in use when reading/writing text
	 * files.
	 * 
	 * @return String
	 * @throws SftpStatusException
	 */
	public byte[] getCanonicalNewline() throws SftpStatusException {
		if (version <= 3) {
			throw new SftpStatusException(
					SftpStatusException.SSH_FX_OP_UNSUPPORTED,
					"Newline setting not available for SFTP versions <= 3");

		}

		if (!extensions.containsKey("newline"))
			return "\r\n".getBytes();

		return extensions.get("newline");
	}
	
	protected void processSupported2(byte[] data) throws IOException {
		
		ByteArrayReader supportedStructure = new ByteArrayReader(data);

		try {
			supportedAttributeMask = supportedStructure
					.readInt();
			supportedAttributeBits = supportedStructure
					.readInt();
			supportedOpenFileFlags = supportedStructure
					.readInt();
			supportedAccessMask = supportedStructure.readInt();
			maxReadSize = (int) supportedStructure.readInt();
			supportedOpenBlockVector = supportedStructure.readShort();
			supportedBlockVector = supportedStructure.readShort();
			if(supportedStructure.available() >= 4) {
				int count = (int) supportedStructure.readInt();
					for (int i = 0; i < count; i++) {
						String ext = supportedStructure.readString();
						if(Log.isTraceEnabled()) {
							Log.trace("Server supports '" + ext
									+ "' attribute extension");
						}
						supportedAttrExtensions.add(ext);
					}
				}
			if(supportedStructure.available() >= 4) {
				int count = (int) supportedStructure.readInt();
					for (int i = 0; i < count; i++) {
						String ext = supportedStructure.readString();
						if(Log.isTraceEnabled()) {
							Log.trace("Server supports '" + ext
									+ "' extension");
						}
						supportedExtensions.add(ext);
					}
				}
			if(Log.isTraceEnabled()) {
				Log.trace("supported-attribute-mask: "
						+ supportedAttributeMask.toString());
				Log.trace("supported-attribute-bits: "
						+ supportedAttributeBits.toString());
				Log.trace("supported-open-flags: "
						+ supportedOpenFileFlags.toString());
				Log.trace("supported-access-mask: "
						+ supportedAccessMask.toString());
				Log.trace("max-read-size: "
						+ maxReadSize.toString());
			}
		} finally {
			supportedStructure.close();
		}
	}
	
	protected void processSupported(byte[] data) throws IOException {
		
		ByteArrayReader supportedStructure = new ByteArrayReader(data);

		try {
			supportedAttributeMask = supportedStructure
					.readInt();
			supportedAttributeBits = supportedStructure
					.readInt();
			supportedOpenFileFlags = supportedStructure
					.readInt();
			supportedAccessMask = supportedStructure.readInt();
			maxReadSize = (int) supportedStructure.readInt();
			while(supportedStructure.available() >= 4) {
				String ext = supportedStructure.readString();
				if(Log.isTraceEnabled()) {
					Log.trace("Server supports '" + ext
							+ "' extension");
				}
				supportedExtensions.add(ext);
			}
			if(Log.isTraceEnabled()) {
				Log.trace("supported-attribute-mask: "
						+ supportedAttributeMask.toString());
				Log.trace("supported-attribute-bits: "
						+ supportedAttributeBits.toString());
				Log.trace("supported-open-flags: "
						+ supportedOpenFileFlags.toString());
				Log.trace("supported-access-mask: "
						+ supportedAccessMask.toString());
				Log.trace("max-read-size: "
						+ maxReadSize.toString());
			}
		} finally {
			supportedStructure.close();
		}
	}
	
	/**
	 * Allows the default character encoding to be overriden for filename
	 * strings. This method should only be called once the channel has been
	 * initialized, if the version of the protocol is less than or equal to 3
	 * the encoding is defaulted to latin1 as no encoding is specified by the
	 * protocol. If the version is greater than 3 the default encoding will be
	 * UTF-8.
	 * 
	 * @param charset
	 * @throws UnsupportedEncodingException
	 * @throws SshException
	 */
	public void setCharsetEncoding(String charset) throws SshException,
			UnsupportedEncodingException {

		if (version == -1)
			throw new SshException(
					"SFTP Channel must be initialized before setting character set encoding",
					SshException.BAD_API_USAGE);

		String test = "123456890";
		test.getBytes(charset);
		CHARSET_ENCODING = charset;
	}

	/**
	 * Version 4 of the SFTP protocol allows the server to return its maximum
	 * supported version instead of the actual version to be used. This method
	 * returns the value provided by the server, if the servers version is less
	 * than or equal to 3 then this method will return the protocol number in
	 * use, otherwise it returns the maximum version supported by the server.
	 * 
	 * @return int
	 */
	public int getServerVersion() {
		return serverVersion;
	}

	/**
	 * Get the current encoding being used for filename Strings.
	 * 
	 * @return String
	 */
	public String getCharsetEncoding() {
		return CHARSET_ENCODING;
	}

	/**
	 * Does the server support an SFTP extension? This checks the extensions
	 * returned by the server during the SFTP version negotiation.
	 * 
	 * @param name
	 *            String
	 * @return boolean
	 */
	public boolean supportsExtension(String name) {
		return extensions.containsKey(name);
	}

	/**
	 * Get the data value of a supported SFTP extension. Call {@link
	 * supportsExtension(String)} before calling this method to determine if the
	 * extension is available.
	 * 
	 * @param name
	 *            String
	 * @return String
	 */
	public byte[] getExtension(String name) {
		return extensions.get(name);
	}
	
	UnsignedInteger32 nextRequestId() {
		requestId = UnsignedInteger32.add(requestId, 1);
		return requestId;
	}
	
	public void close() {
		responses.clear();
		getSession().close();
	}
	
	public SftpMessage getResponse(UnsignedInteger32 requestId) throws SshException {

		SftpMessage msg;
		MessageHolder holder = new MessageHolder();
		holder.msg = responses.get(requestId);
		while (holder.msg == null) {
			try {
				// Read the next response message
				if (sync.requestBlock(requestId, holder)) {
					try {
						msg = new SftpMessage(nextMessage());
						responses.put(new UnsignedInteger32(msg.getMessageId()),msg);
						if(Log.isTraceEnabled()) {
							Log.trace("There are " + responses.size() + " SFTP responses waiting to be processed");
						}
					} finally {
						sync.releaseBlock();
					}
				}
			} catch (InterruptedException e) {
				close();
				throw new SshException("The thread was interrupted",
						SshException.CHANNEL_FAILURE);
			} catch (IOException ex) {
				throw new SshException(SshException.INTERNAL_ERROR, ex);
			} 
		}

		SftpMessage m = (SftpMessage) responses.remove(requestId);
		
		return m;

	}
	
	/**
	 * Change the permissions of a file.
	 * 
	 * @param file
	 *            the file
	 * @param permissions
	 *            an integer value containing a file permissions mask
	 * @throws SshException
	 *             ,SftpStatusException
	 * @deprecated
	 * @see #changePermissions(SftpFile, PosixPermissions)}
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public void changePermissions(byte[] file, int permissions)
			throws SftpStatusException, SshException {
		changePermissions(file, PosixPermissionsBuilder.create().fromBitmask(permissions).build());
	}

	/**
	 * Change the permissions of a file.
	 * 
	 * @param filename
	 *            the path to the file.
	 * @param permissions
	 *            an integer value containing a file permissions mask.
	 * 
	 * @throws SshException
	 *             ,SftpStatusException
	 * @deprecated
	 * @see #changePermissions(String, PosixPermissions)}
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public void changePermissions(String filename, int permissions)
			throws SftpStatusException, SshException {
		changePermissions(filename, PosixPermissionsBuilder.create().fromBitmask(permissions).build());
	}
	
	/**
	 * Change the permissions of a file.
	 * 
	 * @param handle
	 *            the file
	 * @param permissions
	 *            permissions set.
	 * @throws SshException
	 *             ,SftpStatusException
	 */
	public void changePermissions(byte[] handle, PosixPermissions permissions)
			throws SftpStatusException, SshException {
		var bldr = SftpFileAttributesBuilder.ofType(
				SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
				getCharsetEncoding());
		bldr.withPermissions(permissions);
		setAttributes(handle, bldr.build());
	}

	/**
	 * Change the permissions of a file.
	 * 
	 * @param filename
	 *            the path to the file.
	 * @param permissions
	 *            permissions set.
	 * 
	 * @throws SshException
	 *             ,SftpStatusException
	 */
	public void changePermissions(String filename, PosixPermissions permissions)
			throws SftpStatusException, SshException {
		var bldr = SftpFileAttributesBuilder.ofType(
				SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
				getCharsetEncoding());
		bldr.withPermissions(permissions);
		setAttributes(filename, bldr.build());
	}

	/**
	 * Change the permissions of a file.
	 * 
	 * @param filename
	 *            the path to the file.
	 * @param permissions
	 *            a string containing the permissions, for example "rw-r--r--"
	 * 
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void changePermissions(String filename, String permissions)
			throws SftpStatusException, SshException {

		var attrs = SftpFileAttributesBuilder.ofType(
				SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
				getCharsetEncoding());
		attrs.withPermissions(PosixPermissionsBuilder.create().fromFileModeString(permissions).build());
		setAttributes(filename, attrs.build());

	}

	/**
	 * Verify that an OK status has been returned for a request id.
	 * 
	 * @param requestId
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void getOKRequestStatus(UnsignedInteger32 requestId, String path)
			throws SftpStatusException, SshException {

		SftpMessage bar = getResponse(requestId);
		try {
			if (bar.getType() == SSH_FXP_STATUS) {
				processStatusResponse(bar, path);
				return;
			}
			close();
			throw new SshException(
					"The server responded with an unexpected message!",
					SshException.CHANNEL_FAILURE);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		} finally {
			bar.release();
		}

	}

	/**
	 * Set the attributes of a file.
	 * 
	 * @param path the path to the file
	 * @param attrs the file attributes
	 * @throws SftpStatusException
	 * @throws SshException
	 * @deprecated
	 * @see SftpFile#attributes(SftpFileAttributes)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public void setAttributes(SftpFile path, SftpFileAttributes attrs)
			throws SftpStatusException, SshException {
		path.attributes(attrs);
	}

	/**
	 * Sets the attributes of a file.
	 * 
	 * @param path
	 *            the path to the file.
	 * @param attrs
	 *            the file attributes.
	 * @throws SftpStatusException
	 *             , SshException
//	 * @deprecated
	 * @see SftpFile#attributes(SftpFileAttributes)
	 */
//	@Deprecated(since = "3.1.0", forRemoval = true)
	public void setAttributes(String path, SftpFileAttributes attrs)
			throws SftpStatusException, SshException {
		try {
			UnsignedInteger32 requestId = nextRequestId();

			Packet msg = createPacket();
			msg.write(SSH_FXP_SETSTAT);
			msg.writeInt(requestId.longValue());
			msg.writeString(path, CHARSET_ENCODING);
			msg.write(attrs.toByteArray(getVersion()));

			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_SETSTAT for {}", path);
			}
			
			sendMessage(msg);

			getOKRequestStatus(requestId, path);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex, SshException.INTERNAL_ERROR);
		}
	}

	/**
	 * Sets the attributes of a file.
	 * 
	 * @param handle
	 *            the file object.
	 * @param attrs
	 *            the new attributes.
	 * 
	 * @throws SshException
	 * @deprecated
	 * @see SftpHandle#setAttributes(byte[], SftpFileAttributes)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public void setAttributes(byte[] handle, SftpFileAttributes attrs)
			throws SftpStatusException, SshException {
		if (!isValidHandle(handle)) {
			throw new SftpStatusException(SftpStatusException.INVALID_HANDLE,
					"The handle is not an open file handle!");
		}
		getBestHandle(handle).setAttributes(attrs);
	}

	/**
	 * Send a write request for an open file but do not wait for the response
	 * from the server.
	 * 
	 * @param handle
	 * @param position
	 * @param data
	 * @param off
	 * @param len
	 * @return UnsignedInteger32
	 * @throws SshException
	 * @deprecated
	 * @see SftpHandle#postWriteRequest(long, byte[], int, int)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public UnsignedInteger32 postWriteRequest(byte[] handle, long position,
			byte[] data, int off, int len) throws SftpStatusException,
			SshException {
		return getBestHandle(handle).postWriteRequest(position, data, off, len);
	}

	/**
	 * Write a block of data to an open file.
	 * 
	 * @param handle
	 *            the open file handle.
	 * @param offset
	 *            the offset in the file to start writing
	 * @param data
	 *            a buffer containing the data to write
	 * @param off
	 *            the offset to start in the buffer
	 * @param len
	 *            the length of data to write (setting to false will increase
	 *            file transfer but may miss errors)
	 * @throws SshException
	 */
	public void writeFile(byte[] handle, UnsignedInteger64 offset, byte[] data,
			int off, int len) throws SftpStatusException, SshException {

		SftpHandle h = getBestHandle(handle);
		getOKRequestStatus(h.postWriteRequest(offset.longValue(), data, off, len),
				h.getFile() == null ? "<fileless-handle>" : h.getFile().getAbsolutePath());
	}

	/**
	 * Performs an optimized write of a file through asynchronous messaging and
	 * through buffering the local file into memory.
	 * 
	 * @param handle
	 *            the open file handle to write to
	 * @param blocksize
	 *            the block size to send data, should be between 4096 and 65536
	 * @param outstandingRequests
	 *            the maximum number of requests that can be outstanding at any
	 *            one time
	 * @param in
	 *            the InputStream to read from
	 * @param buffersize
	 *            the size of the temporary buffer to read from the InputStream.
	 *            Data is buffered into a temporary buffer so that the number of
	 *            local filesystem reads is reducted to a minimum. This
	 *            increases performance and so the buffer size should be as high
	 *            as possible. The default operation, if buffersize <= 0 is to
	 *            allocate a buffer the same size as the blocksize, meaning no
	 *            buffer optimization is performed.
	 * @param progress
	 *            provides progress information, may be null.
	 * @throws SshException
	 * @deprecated
	 * @see SftpHandle#performOptimizedWrite(String, int, int, java.io.InputStream, int, FileTransferProgress, long)
	 */
	public void performOptimizedWrite(String filename, byte[] handle, int blocksize,
			int maxAsyncRequests, java.io.InputStream in, int buffersize,
			FileTransferProgress progress) throws SftpStatusException,
			SshException, TransferCancelledException {
		performOptimizedWrite(filename, handle, blocksize, maxAsyncRequests, in,
				buffersize, progress, 0);
	}

	/**
	 * Performs an optimized write of a file through asynchronous messaging and
	 * through buffering the local file into memory.
	 * 
	 * @param handle
	 *            the open file handle to write to
	 * @param blocksize
	 *            the block size to send data, should be between 4096 and 65536
	 * @param outstandingRequests
	 *            the maximum number of requests that can be outstanding at any
	 *            one time
	 * @param in
	 *            the InputStream to read from
	 * @param buffersize
	 *            the size of the temporary buffer to read from the InputStream.
	 *            Data is buffered into a temporary buffer so that the number of
	 *            local filesystem reads is reducted to a minimum. This
	 *            increases performance and so the buffer size should be as high
	 *            as possible. The default operation, if buffersize <= 0 is to
	 *            allocate a buffer the same size as the blocksize, meaning no
	 *            buffer optimization is performed.
	 * @param progress
	 *            provides progress information, may be null.
	 * @param position
	 *            the position in the file to start writing to.
	 * @throws SshException
	 * @deprecated
	 * @see SftpHandle#performOptimizedWrite(String, int, int, java.io.InputStream, int, FileTransferProgress, long)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public void performOptimizedWrite(String filename, byte[] handle, int blocksize,
			int maxAsyncRequests, java.io.InputStream in, int buffersize,
			FileTransferProgress progress, long position)
			throws SftpStatusException, SshException,
			TransferCancelledException {
		getBestHandle(handle).performOptimizedWrite(filename, blocksize, maxAsyncRequests, in, buffersize, progress, position);
	}

	/**
	 * Performs an optimized read of a file through use of asynchronous
	 * messages. The total number of outstanding read requests is configurable.
	 * This should be safe on file objects as the SSH protocol states that file
	 * read operations should return the exact number of bytes requested in each
	 * request. However the server is not required to return the exact number of
	 * bytes on device files and so this method should not be used for device
	 * files.
	 * 
	 * @param handle
	 *            the open files handle
	 * @param length
	 *            the length of the file
	 * @param blocksize
	 *            the blocksize to read
	 * @param out
	 *            an OutputStream to output the file into
	 * @param outstandingRequests
	 *            the maximum number of read requests to
	 * @param progress
	 * @throws SshException
	 * @deprecated
	 * @see SftpHandle#performOptimizedRead(String, long, int, OutputStream, int, FileTransferProgress, long)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public void performOptimizedRead(String filename, byte[] handle, long length, int blocksize,
			java.io.OutputStream out, int outstandingRequests,
			FileTransferProgress progress) throws SftpStatusException,
			SshException, TransferCancelledException {

		performOptimizedRead(filename, handle, length, blocksize, out,
				outstandingRequests, progress, 0);
	}

	/**
	 * Performs an optimized read of a file through use of asynchronous
	 * messages. The total number of outstanding read requests is configurable.
	 * This should be safe on file objects as the SSH protocol states that file
	 * read operations should return the exact number of bytes requested in each
	 * request. However the server is not required to return the exact number of
	 * bytes on device files and so this method should not be used for device
	 * files.
	 * 
	 * @param handle
	 *            the open files handle
	 * @param length
	 *            the amount of the file file to be read, equal to the file
	 *            length when reading the whole file
	 * @param blocksize
	 *            the blocksize to read
	 * @param out
	 *            an OutputStream to output the file into
	 * @param outstandingRequests
	 *            the maximum number of read requests to
	 * @param progress
	 * @param position
	 *            the postition from which to start reading the file
	 * @throws SshException
	 * @deprecated
	 * @see SftpHandle#performOptimizedRead(String, long, int, OutputStream, int, FileTransferProgress, long)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public void performOptimizedRead(String filename, byte[] handle, long length, int blocksize,
			OutputStream out, int outstandingRequests,
			FileTransferProgress progress, long position)
			throws SftpStatusException, SshException,
			TransferCancelledException {
		
		getBestHandle(handle).performOptimizedRead(length, blocksize, out, outstandingRequests, progress, position);

	}

	/**
	 * Perform a synchronous read of a file from the remote file system. This
	 * implementation waits for acknowledgement of every data packet before
	 * requesting additional data.
	 * 
	 * @param handle
	 * @param blocksize
	 * @param out
	 * @param progress
	 * @param position
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 * @deprecated
	 * @see SftpHandle#performSynchronousRead(int, OutputStream, FileTransferProgress, long)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public void performSynchronousRead(byte[] handle, int blocksize,
			OutputStream out, FileTransferProgress progress, long position)
			throws SftpStatusException, SshException,
			TransferCancelledException {
		getBestHandle(handle).performSynchronousRead(blocksize, out, progress, position);
	}

	/**
	 * Post a read request to the server and return the request id; this is used
	 * to optimize file downloads. In normal operation the files are transfered
	 * by using a synchronous set of requests, however this slows the download
	 * as the client has to wait for the servers response before sending another
	 * request.
	 * 
	 * @param handle
	 * @param offset
	 * @param len
	 * @return UnsignedInteger32
	 * @throws SshException
	 * @deprecated
	 * @see SftpHandle#postReadRequest(long, int)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public UnsignedInteger32 postReadRequest(byte[] handle, long offset, int len)
			throws SftpStatusException, SshException {
		return getBestHandle(handle).postReadRequest(offset, len);
	}

	/**
	 * Read a block of data from an open file.
	 * 
	 * @param handle
	 *            the open file handle
	 * @param offset
	 *            the offset to start reading in the file
	 * @param output
	 *            a buffer to write the returned data to
	 * @param off
	 *            the starting offset in the output buffer
	 * @param len
	 *            the length of data to read
	 * @return int
	 * @throws SshException
	 * @deprecated
	 * @see SftpHandle#readFile(UnsignedInteger64, byte[], int, int)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public int readFile(byte[] handle, UnsignedInteger64 offset, byte[] output,
			int off, int len) throws SftpStatusException, SshException {
		return getBestHandle(handle).readFile(offset, output, off, len);
	}

	/**
	 * Utility method to obtain an {@link SftpFile} instance for a given path.
	 * 
	 * @param path
	 * @return SftpFile
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFile getFile(String path) throws SftpStatusException,
			SshException {
		String absolute = getAbsolutePath(path);
		return new SftpFile(absolute, getAttributes(absolute), this, null);
	}

	/**
	 * Get the absolute path of a file.
	 * 
	 * @param file
	 * @return String
	 * @throws SshException
	 */
	public String getAbsolutePath(SftpFile file) throws SftpStatusException,
			SshException {
		return getAbsolutePath(file.getFilename());
	}

	/**
	 * Lock file.
	 * 
	 * @param handle
	 * @param offset
	 * @param length
	 * @param lockFlags
	 * @throws SftpStatusException
	 * @throws SshException
	 * @deprecated
	 * @see SftpHandle#lock()
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public void lockFile(byte[] handle, long offset, long length, int lockFlags) throws SftpStatusException, SshException {
		
		if(version < 6) {
			throw new SftpStatusException(
					SftpStatusException.SSH_FX_OP_UNSUPPORTED,
					"Locks are not supported by the server SFTP version "
							+ String.valueOf(version));
		}
		
		SftpHandle h = getBestHandle(handle);
		
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_BLOCK);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			msg.writeUINT64(offset);
			msg.writeUINT64(length);
			msg.writeInt(lockFlags);
			
			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_BLOCK for {}", h.getFile().getAbsolutePath());
			}
			sendMessage(msg);

			getOKRequestStatus(requestId, h.getFile().getAbsolutePath());
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}

	/**
	 * Lock file.
	 * 
	 * @param handle
	 * @param offset
	 * @param length
	 * @param lockFlags
	 * @throws SftpStatusException
	 * @throws SshException
	 * @deprecated
	 * @see SftpHandle#lock()
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public void unlockFile(byte[] handle, long offset, long length) throws SftpStatusException, SshException {
		if(version < 6) {
			throw new SftpStatusException(
					SftpStatusException.SSH_FX_OP_UNSUPPORTED,
					"Locks are not supported by the server SFTP version "
							+ String.valueOf(version));
		}
		
		SftpHandle h = getBestHandle(handle);
		
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_UNBLOCK);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			msg.writeUINT64(offset);
			msg.writeUINT64(length);
			
			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_UNBLOCK for {}", h.getFile().getAbsolutePath());
			}
			sendMessage(msg);

			getOKRequestStatus(requestId, h.getFile().getAbsolutePath());
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}
	/**
	 * Create a symbolic link.
	 * 
	 * @param targetpath
	 *            the symbolic link to create
	 * @param linkpath
	 *            the path to which the symbolic link points
	 * @throws SshException
	 *             if the remote SFTP version is < 3 an exception is thrown as
	 *             this feature is not supported by previous versions of the
	 *             protocol.
	 */
	public void createSymbolicLink(String targetpath, String linkpath)
			throws SftpStatusException, SshException {

		if (version < 3) {
			throw new SftpStatusException(
					SftpStatusException.SSH_FX_OP_UNSUPPORTED,
					"Symbolic links are not supported by the server SFTP version "
							+ String.valueOf(version));
		}
		
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_SYMLINK);
			msg.writeInt(requestId.longValue());
			msg.writeString(linkpath, CHARSET_ENCODING);
			msg.writeString(targetpath, CHARSET_ENCODING);
			if(version >= 6) {
				msg.writeBoolean(true);
			}
			
			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_SYMLINK to link {} to {}", 
						linkpath,
						targetpath);
			}

			sendMessage(msg);

			getOKRequestStatus(requestId, linkpath);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

	}
	
	/**
	 * Create a symbolic link.
	 * 
	 * @param targetpath
	 *            the symbolic link to create
	 * @param linkpath
	 *            the path to which the symbolic link points
	 * @throws SshException
	 *             if the remote SFTP version is < 3 an exception is thrown as
	 *             this feature is not supported by previous versions of the
	 *             protocol.
	 */
	public void createLink(String targetpath, String linkpath, boolean symbolic)
			throws SftpStatusException, SshException {

		if (version < 6 && !symbolic) {
			throw new SftpStatusException(
					SftpStatusException.SSH_FX_OP_UNSUPPORTED,
					"Hard links are not supported by the server SFTP version "
							+ String.valueOf(version));
		}
		
		if(version < 6 && symbolic) {
			createSymbolicLink(targetpath, linkpath);
			return;
		}
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_LINK);
			msg.writeInt(requestId.longValue());
			msg.writeString(linkpath, CHARSET_ENCODING);
			msg.writeString(targetpath, CHARSET_ENCODING);
			msg.writeBoolean(symbolic);

			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_LINK to link {} to {}", 
						linkpath,
						targetpath);
			}
			sendMessage(msg);

			getOKRequestStatus(requestId, linkpath);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

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
	public String getSymbolicLinkTarget(String linkpath)
			throws SftpStatusException, SshException {

		if (version < 3) {
			throw new SftpStatusException(
					SftpStatusException.SSH_FX_OP_UNSUPPORTED,
					"Symbolic links are not supported by the server SFTP version "
							+ String.valueOf(version));
		}

		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_READLINK);
			msg.writeInt(requestId.longValue());
			msg.writeString(linkpath, CHARSET_ENCODING);

			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_READLINK for {}", linkpath);
			}
			
			sendMessage(msg);

			SftpMessage fileMsg = getResponse(requestId);
			if (fileMsg.getType() == SSH_FXP_STATUS) {
				processStatusResponse(fileMsg, linkpath);
				throw new IllegalStateException("Received unexpected SSH_FX_OK in status response!");
			} else {
				try {
					SftpFile[] files = extractFiles(fileMsg, null);
					return files[0].getAbsolutePath();
				} finally {
					fileMsg.release();
				}
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

	}

	/**
	 * Gets the users default directory.
	 * 
	 * @return String
	 * @throws SshException
	 */
	public String getDefaultDirectory() throws SftpStatusException,
			SshException {
		return getAbsolutePath("");
	}

	/**
	 * Get the absolute path of a file.
	 * 
	 * @param path
	 * @return String
	 * @throws SshException
	 */
	public String getAbsolutePath(String path) throws SftpStatusException,
			SshException {
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_REALPATH);
			msg.writeInt(requestId.longValue());
			msg.writeString(path, CHARSET_ENCODING);
			
			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_REALPATH for {}", path);
			}
			
			sendMessage(msg);

			return getSingleFileResponse(getResponse(requestId), "SSH_FXP_REALPATH", path).getAbsolutePath();
			
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

	}
	
	/**
	 * Get a single SftpFile object from a message returning SSH_FXP_NAME result.
	 * @param bar
	 * @param messageName
	 * @return
	 * @throws SshException
	 * @throws SftpStatusException
	 */
	public SftpFile getSingleFileResponse(SftpMessage bar, String messageName, String path) throws SshException, SftpStatusException {
		try {
			if (bar.getType() == SSH_FXP_NAME) {
				SftpFile[] files = extractFiles(bar, null);

				if (files.length != 1) {
					close();
					throw new SshException(
							"Server responded to "
							+ messageName + " with too many files!",
							SshException.CHANNEL_FAILURE);
				}

				if(Log.isDebugEnabled()) {
					Log.debug("Received SSH_FXP_NAME with value {}", files[0].getAbsolutePath());
				}
				return files[0];
			} else if (bar.getType() == SSH_FXP_STATUS) {
				processStatusResponse(bar, path);
				throw new IllegalStateException("Received unexpected SSH_FX_OK in status response!");
			} else {
				close();
				throw new SshException(
						"The server responded with an unexpected message",
						SshException.CHANNEL_FAILURE);
			}
		} catch (IOException e) {
			throw new SshException(e);
		}
	}

	/**
	 * <p>
	 * List the children of a directory.
	 * </p>
	 * <p>
	 * To use this method first open a directory with the <a
	 * href="#openDirectory(java.lang.String)"> openDirectory</a> method and
	 * then create a Vector to store the results. To retrieve the results keep
	 * calling this method until it returns -1 which indicates no more results
	 * will be returned. <blockquote>
	 * 
	 * <pre>
	 * SftpFile dir = sftp.openDirectory(&quot;code/foobar&quot;);
	 * Vector results = new Vector();
	 * while (sftp.listChildren(dir, results) &gt; -1)
	 * 	;
	 * sftp.closeFile(dir);
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * </p>
	 * 
	 * @param file
	 * @param children
	 * @return int
	 * @throws SftpStatusException
	 *             , SshException
	 * @deprecated
	 * @see SftpHandle#listChildren(List)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public int listChildren(SftpFile file, List<SftpFile> children)
			throws SftpStatusException, SshException {
		if (file.isDirectory()) {
			return openDirectory(file.getAbsolutePath()).listChildren(children);
		} else {
			throw new SshException("Cannot list children for this file object",
					SshException.BAD_API_USAGE);
		}
	}

	SftpFile[] extractFiles(SftpMessage bar, String parent) throws SshException {

		try {

			if (parent != null && !parent.endsWith("/")) {
				parent += "/";
			}

			int count = (int) bar.readInt();
			SftpFile[] files = new SftpFile[count];

			String shortname;
			String longname = null;

			for (int i = 0; i < files.length; i++) {
				shortname = bar.readString(CHARSET_ENCODING);

				if (version <= 3) {
					// read and throw away the longname as don't use it but need
					// to read it out of the bar to advance the position.
					longname = bar.readString(CHARSET_ENCODING);
				}

				var bldr = SftpFileAttributesBuilder.of(bar, getVersion(), getCharsetEncoding());

				// Work out username/group from long name
				if (longname != null && version <= 3) {
					try {
						StringTokenizer t = new StringTokenizer(longname);
						t.nextToken();
						t.nextToken();
						String username = t.nextToken();
						String group = t.nextToken();

						bldr.withUsername(username);
						bldr.withGroup(group);

					} catch (Exception e) {

					}

				}

				files[i] = new SftpFile(parent != null ? parent + shortname
						: shortname, bldr.build(), this, longname);
			}

			return files;
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}

	/**
	 * Recurse through a hierarchy of directories creating them as necessary.
	 * 
	 * @param path
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void recurseMakeDirectory(String path) throws SftpStatusException,
			SshException {
		SftpHandle file;

		if (path.trim().length() > 0) {
			try {
				file = openDirectory(path);
				file.close();
			} catch (SshException ioe) {

				int idx = 0;

				do {

					idx = path.indexOf('/', idx);
					String tmp = (idx > -1 ? path.substring(0, idx + 1) : path);
					try {
						file = openDirectory(tmp);
						try {
							file.close();
						} catch (IOException e) {
							throw new SshException(e);
						}
					} catch (SshException ioe7) {
						makeDirectory(tmp);
					} 

				} while (idx > -1);

			} catch (IOException ex) {
				throw new SshException(ex);
			}
		}
	}

	/**
	 * Open a file.
	 * 
	 * @param absolutePath
	 * @param flags
	 * @return SftpFile
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public SftpHandle openFile(String absolutePath, int flags)
			throws SftpStatusException, SshException {
		return openFile(absolutePath, flags, SftpFileAttributesBuilder.ofType(
				SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
				getCharsetEncoding()).build());
	}

	/**
	 * Open a file.
	 * 
	 * @param path
	 * @param flags
	 * @param attrs
	 * @return SftpFile
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public SftpHandle openFile(String path, int flags,
			SftpFileAttributes attrs) throws SftpStatusException, SshException {

		if (version >= 5) {

			if(Log.isTraceEnabled()) {
				Log.trace("Converting openFile request to version 5+ format");
			}
			// Translate old flags and access to new values
			int accessFlags = 0;
			int newFlags = 0;

			if ((flags & OPEN_READ) == OPEN_READ) {
				accessFlags |= ACL.ACE4_READ_DATA | ACL.ACE4_READ_ATTRIBUTES;
				if(Log.isTraceEnabled()) {
					Log.trace("OPEN_READ present, adding ACE4_READ_DATA, ACE4_READ_ATTRIBUTES");
				}
			}
			if ((flags & OPEN_WRITE) == OPEN_WRITE) {
				accessFlags |= ACL.ACE4_WRITE_DATA;
				accessFlags |= ACL.ACE4_WRITE_ATTRIBUTES;
				if(Log.isTraceEnabled()) {
					Log.trace("OPEN_WRITE present, adding ACE4_WRITE_DATA, ACE4_WRITE_ATTRIBUTES ");
				}
			}
			if ((flags & OPEN_APPEND) == OPEN_APPEND) {
				accessFlags |= ACL.ACE4_APPEND_DATA;
				accessFlags |= ACL.ACE4_WRITE_DATA;
				accessFlags |= ACL.ACE4_WRITE_ATTRIBUTES;
				newFlags |= SSH_FXF_ACCESS_APPEND_DATA;
				if(Log.isTraceEnabled()) {
					Log.trace("OPEN_APPEND present, adding ACE4_APPEND_DATA,ACE4_WRITE_DATA, ACE4_WRITE_ATTRIBUTES");
				}
			}
			
			if((flags & OPEN_EXCLUSIVE)==OPEN_EXCLUSIVE) {
				newFlags |= SSH_FXF_CREATE_NEW;
				if(Log.isTraceEnabled()) {
					Log.trace("OPEN_EXCLUSIVE present, adding SSH_FXF_CREATE_NEW");
				}
			} 
			else if((flags & OPEN_CREATE)==OPEN_CREATE) {
				if((flags & OPEN_TRUNCATE)==OPEN_TRUNCATE) {
					newFlags |= SSH_FXF_CREATE_TRUNCATE;
					if(Log.isTraceEnabled()) {
						Log.trace("OPEN_CREATE and OPEN_TRUNCATE present, adding SSH_FXF_CREATE_TRUNCATE");
					}
				} 
				else {
					newFlags |= SSH_FXF_OPEN_OR_CREATE;
				}
			} else {
				if((flags & OPEN_TRUNCATE)==OPEN_TRUNCATE) {
					newFlags |= SSH_FXF_TRUNCATE_EXISTING;
					if(Log.isTraceEnabled()) {
						Log.trace("OPEN_TRUNCATE present, adding SSH_FXF_TRUNCATE_EXISTING");
					}
				} 
				else {
					newFlags |= SSH_FXF_OPEN_EXISTING;
				}
			}
			
			if((flags & OPEN_TEXT)==OPEN_TEXT) {
				newFlags |= SSH_FXF_ACCESS_TEXT_MODE;
				if(Log.isTraceEnabled()) {
					Log.trace("OPEN_TEXT present adding SSH_FXF_ACCESS_TEXT_MODE");
				}
			}
			
			return openFileVersion5(path, newFlags, accessFlags, attrs);
		} else {
			if (attrs == null) {
				attrs = SftpFileAttributesBuilder.ofType(
						SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
						getCharsetEncoding()).build();
			}

			try {

				SftpFileAttributes attributes = getAttributes(path);
				
				UnsignedInteger32 requestId = nextRequestId();
				Packet msg = createPacket();
				msg.write(SSH_FXP_OPEN);
				msg.writeInt(requestId.longValue());
				msg.writeString(path, CHARSET_ENCODING);
				msg.writeInt(flags);
				msg.write(attrs.toByteArray(getVersion()));

				if(Log.isDebugEnabled()) {
					Log.debug("Sending SSH_FXP_OPEN for {}", path);
				}
				
				sendMessage(msg);


				SftpFile file = new SftpFile(path, attributes, this, null);
				SftpHandle handle = file.handle(getHandleResponse(requestId, path));

				EventServiceImplementation.getInstance().fireEvent(
						(new Event(this,
								EventCodes.EVENT_SFTP_FILE_OPENED, true))
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										file.getAbsolutePath()));
				return handle;
			} catch (SshIOException ex) {
				throw ex.getRealException();
			} catch (IOException ex) {
				throw new SshException(ex);
			}
		}
	}

	public SftpHandle openFileVersion5(String path, int flags,
			int accessFlags, SftpFileAttributes attrs)
			throws SftpStatusException, SshException {

		if (attrs == null) {
			attrs = SftpFileAttributesBuilder.ofType(
					SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
					getCharsetEncoding()).build();
		}

		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_OPEN);
			msg.writeInt(requestId.longValue());
			msg.writeString(path, CHARSET_ENCODING);
			msg.writeInt(accessFlags);
			msg.writeInt(flags);
			msg.write(attrs.toByteArray(getVersion()));

			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_OPEN for {}", path);
			}
			
			sendMessage(msg);

			SftpFile file = new SftpFile(path, getAttributes(path), this, null);
			SftpHandle handle = file.handle(getHandleResponse(requestId, path));

			EventServiceImplementation.getInstance().fireEvent(
					(new Event(this, EventCodes.EVENT_SFTP_FILE_OPENED,
							true)).addAttribute(
							EventCodes.ATTRIBUTE_FILE_NAME,
							file.getAbsolutePath()));
			return handle;
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}

	/**
	 * Open a directory.
	 * 
	 * @param path
	 * @return sftpfile
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public SftpHandle openDirectory(String path) throws SftpStatusException,
			SshException {

		String absolutePath = getAbsolutePath(path);

		SftpFileAttributes attrs = getAttributes(absolutePath);

		if (!attrs.isDirectory()) {
			throw new SftpStatusException(SftpStatusException.SSH_FX_FAILURE,
					path + " is not a directory");
		}

		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_OPENDIR);
			msg.writeInt(requestId.longValue());
			msg.writeString(absolutePath, CHARSET_ENCODING);
			
			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_OPENDIR for {}", path);
			}
			
			sendMessage(msg);
			
			return new SftpFile(absolutePath, attrs, this, null).handle(getHandleResponse(requestId, path));
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

	}

	@Deprecated(since = "3.1.0")
	public void closeHandle(byte[] handle) throws SftpStatusException, SshException {
		if (handle == null) {
			throw new SftpStatusException(SftpStatusException.INVALID_HANDLE,
					"The handle is invalid!");
		}

		try {
			getBestHandle(handle).close();
		} catch (IOException ex) {
			if(ex.getCause() instanceof SshException)
				throw (SshException)ex.getCause();
			if(ex.getCause() instanceof SftpStatusException)
				throw (SftpStatusException)ex.getCause();
			else
				throw new SshException(ex);
		}
	}
	
	SftpHandle getBestHandle(byte[] handle) {
		synchronized(handles) {
			var h = handles.get(handle);
			if(h != null) {
				return h;
			}
		}
		return new SftpHandle(handle, this, null);
	}

	/**
	 * Close a file or directory.
	 * 
	 * @param file
	 * @throws SftpStatusException
	 *             , SshException
	 */
	@Deprecated
	public void closeFile(SftpHandle file) throws IOException {
		file.close();
	}

	private boolean isValidHandle(byte[] handle) {
		return handle != null;
	}

	/**
	 * Remove an empty directory.
	 * 
	 * @param path
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void removeDirectory(String path) throws SftpStatusException,
			SshException {
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_RMDIR);
			msg.writeInt(requestId.longValue());
			msg.writeString(path, CHARSET_ENCODING);

			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_RMDIR for {}", path);
			}
			
			sendMessage(msg);

			getOKRequestStatus(requestId, path);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
		EventServiceImplementation.getInstance().fireEvent(
				(new Event(this, EventCodes.EVENT_SFTP_DIRECTORY_DELETED,
						true)).addAttribute(
						EventCodes.ATTRIBUTE_DIRECTORY_PATH, path));
	}

	/**
	 * Remove a file.
	 * 
	 * @param path
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void removeFile(String path) throws SftpStatusException,
			SshException {
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_REMOVE);
			msg.writeInt(requestId.longValue());
			msg.writeString(path, CHARSET_ENCODING);

			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_REMOVE for {}", path);
			}
			
			sendMessage(msg);

			getOKRequestStatus(requestId, path);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
		EventServiceImplementation.getInstance()
				.fireEvent(
						(new Event(this,
								EventCodes.EVENT_SFTP_FILE_DELETED, true))
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										path));
	}

	/**
	 * Rename an existing file.
	 * 
	 * @param oldpath
	 * @param newpath
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void renameFile(String oldpath, String newpath)
			throws SftpStatusException, SshException {
		renameFile(oldpath, newpath, 0);
	}
	public void renameFile(String oldpath, String newpath, int flags)
			throws SftpStatusException, SshException {

		if (version < 2) {
			throw new SftpStatusException(
					SftpStatusException.SSH_FX_OP_UNSUPPORTED,
					"Renaming files is not supported by the server SFTP version "
							+ String.valueOf(version));
		}
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_RENAME);
			msg.writeInt(requestId.longValue());
			msg.writeString(oldpath, CHARSET_ENCODING);
			msg.writeString(newpath, CHARSET_ENCODING);

			if(version >= 5) {
				msg.writeInt(flags);
			}
			
			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_RENAME from {} to {}", oldpath, newpath);
			}
			sendMessage(msg);

			getOKRequestStatus(requestId, newpath);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
		EventServiceImplementation
				.getInstance()
				.fireEvent(
						(new Event(this,
								EventCodes.EVENT_SFTP_FILE_RENAMED, true))
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										oldpath)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NEW_NAME,
										newpath));
	}

	/**
	 * Get the attributes of a file. This method follows symbolic links
	 * 
	 * @param path
	 * @return SftpFileAttributes
	 * @throws SshException
	 */
	public SftpFileAttributes getAttributes(String path)
			throws SftpStatusException, SshException {
		return getAttributes(path, SSH_FXP_STAT, "SSH_FXP_STAT");
	}

	/**
	 * Get the attributes of a file. This method does not follow symbolic links
	 * so will return the attributes of an actual link, not its target.
	 * 
	 * @param path
	 * @return
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFileAttributes getLinkAttributes(String path)
			throws SftpStatusException, SshException {
		return getAttributes(path, SSH_FXP_LSTAT, "SSH_FXP_LSTAT");
	}

	protected SftpFileAttributes getAttributes(String path, int messageId, String messageName)
			throws SftpStatusException, SshException {
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(messageId);
			msg.writeInt(requestId.longValue());
			msg.writeString(path, CHARSET_ENCODING);

			if (version > 3) {
				
				long flags = SftpFileAttributes.SSH_FILEXFER_ATTR_SIZE
						| SftpFileAttributes.SSH_FILEXFER_ATTR_PERMISSIONS
						| SftpFileAttributes.SSH_FILEXFER_ATTR_ACCESSTIME
						| SftpFileAttributes.SSH_FILEXFER_ATTR_CREATETIME
						| SftpFileAttributes.SSH_FILEXFER_ATTR_MODIFYTIME
						| SftpFileAttributes.SSH_FILEXFER_ATTR_ACL
						| SftpFileAttributes.SSH_FILEXFER_ATTR_OWNERGROUP
						| SftpFileAttributes.SSH_FILEXFER_ATTR_SUBSECOND_TIMES;
				
				if(version > 4) {
					flags |= SftpFileAttributes.SSH_FILEXFER_ATTR_BITS;
				}
				
				msg.writeInt(flags);
			}
			
			if(Log.isDebugEnabled()) {
				Log.debug("Sending {} for {}", 
						messageName,
						path);
			}

			sendMessage(msg);

			SftpMessage bar = getResponse(requestId);
			try {
				return extractAttributes(bar, path);
			} finally {
				bar.release();
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}

	SftpFileAttributes extractAttributes(SftpMessage bar, String path)
			throws SftpStatusException, SshException {
		try {
			if (bar.getType() == SSH_FXP_ATTRS) {
				if(Log.isDebugEnabled()) {
					Log.debug("Received SSH_FXP_ATTRS for {}", path);
				}
				return SftpFileAttributesBuilder.of(bar, getVersion(), getCharsetEncoding()).build();
			} else if (bar.getType() == SSH_FXP_STATUS) {
				processStatusResponse(bar, path);
				throw new IllegalStateException("Received unexpected SSH_FX_OK in status response!");
			} else {
				close();
				throw new SshException(
						"The server responded with an unexpected message.",
						SshException.CHANNEL_FAILURE);
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}

	void processStatusResponse(SftpMessage bar, String path) throws SftpStatusException, IOException {
		
		int status = (int) bar.readInt();

		if(status == SftpStatusException.SSH_FX_OK) {
			
			if(Log.isDebugEnabled()) {
				Log.debug("Received SSH_FX_OK for {}", 
						SftpStatusException.getStatusMessage(status),
						path);
			}
			return;
		}
		
		if (version >= 3) {
			String desc = bar.readString();
			if(Log.isDebugEnabled()) {
				Log.debug("Received {} with message {}", 
						SftpStatusException.getStatusMessage(status), desc);
			}
			throw new SftpStatusException(status, desc);
		}
		
		if(Log.isDebugEnabled()) {
			Log.debug("Received {}", SftpStatusException.getStatusMessage(status));
		}
		
		throw new SftpStatusException(status);
	}

	/**
	 * Get the attributes of a file.
	 * 
	 * @param file
	 * @return SftpFileAttributes
	 * @throws SftpStatusException
	 *             , SshException
	 * @deprecated
	 * @see SftpHandle#setAttributes(byte[], SftpFileAttributes)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SftpFileAttributes getAttributes(SftpFile file)
			throws SftpStatusException, SshException {
		return getAttributes(file.getAbsolutePath());
	}

	/**
	 * Make a directory. If the directory exists this method will throw an
	 * exception.
	 * 
	 * @param path
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void makeDirectory(String path) throws SftpStatusException,
			SshException {
		makeDirectory(path, SftpFileAttributesBuilder.ofType(
				SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY,
				getCharsetEncoding()).build());
	}

	/**
	 * Make a directory. If the directory exists this method will throw an
	 * exception.
	 * 
	 * @param path
	 * @param attrs
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void makeDirectory(String path, SftpFileAttributes attrs)
			throws SftpStatusException, SshException {
		try {
			UnsignedInteger32 requestId = nextRequestId();

			Packet msg = createPacket();
			msg.write(SSH_FXP_MKDIR);
			msg.writeInt(requestId.longValue());
			msg.writeString(path, CHARSET_ENCODING);
			msg.write(attrs.toByteArray(getVersion()));

			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_MKDIR for {}", 
						path);
			}
			
			sendMessage(msg);

			getOKRequestStatus(requestId, path);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}
	
	SftpHandle getHandle(SftpMessage bar, SftpFile file) 
			throws SftpStatusException, SshException {
		var response = getHandleResponse(bar, file.getAbsolutePath());
		return new SftpHandle(response, this, file);
	}
	
	public SftpMessage getExtendedReply(UnsignedInteger32 requestId, String path) throws SftpStatusException, SshException {
		return getExtendedReply(getResponse(requestId), path);
	}
	
	public SftpMessage getExtendedReply(SftpMessage bar, String path) throws SftpStatusException, SshException {
		try {
			if (bar.getType() == SSH_FXP_EXTENDED_REPLY) {
				if(Log.isDebugEnabled()) {
					Log.debug("Received SSH_FX_EXTENDED_REPLY");
				}
				return bar;
			} else if (bar.getType() == SSH_FXP_STATUS) {
				processStatusResponse(bar, path);
				throw new IllegalStateException("Received unexpected SSH_FX_OK in status response!");
			} else {
				close();
				throw new SshException(
						String.format("The server responded with an unexpected message! id=%d", bar.getType()),
						SshException.CHANNEL_FAILURE);
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}
	
	public byte[] getHandleResponse(UnsignedInteger32 requestId, String path)
			throws SftpStatusException, SshException {
		return getHandleResponse(getResponse(requestId), path);
	}
	
	public SftpHandle getHandle(UnsignedInteger32 requestId, SftpFile file)
			throws SftpStatusException, SshException {
		return new SftpHandle(getHandleResponse(getResponse(requestId), file.getAbsolutePath()), this, file);
	}
	
	public byte[] getHandleResponse(SftpMessage bar, String path)
			throws SftpStatusException, SshException {

		try {
			if (bar.getType() == SSH_FXP_HANDLE) {
				if(Log.isDebugEnabled()) {
					Log.debug("Received SSH_FXP_HANDLE");
				}
				return bar.readBinaryString();
			} else if (bar.getType() == SSH_FXP_STATUS) {
				processStatusResponse(bar, path);
				throw new IllegalStateException("Received unexpected SSH_FX_OK in status response!");
			} else {
				close();
				throw new SshException(
						String.format("The server responded with an unexpected message! id=%d", bar.getType()),
						SshException.CHANNEL_FAILURE);
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}

	SftpMessage getExtensionResponse(UnsignedInteger32 requestId, String path)
			throws SftpStatusException, SshException {

		SftpMessage bar = getResponse(requestId);
		try {
			if (bar.getType() == SSH_FXP_EXTENDED_REPLY) {
				if(Log.isDebugEnabled()) {
					Log.debug("Received SSH_FXP_EXTENDED_REPLY");
				}
				return bar;
			} else if (bar.getType() == SSH_FXP_STATUS) {
				processStatusResponse(bar, path);
				throw new IllegalStateException("Received unexpected SSH_FX_OK in status response!");
			} else {
				close();
				throw new SshException(
						"The server responded with an unexpected message!",
						SshException.CHANNEL_FAILURE);
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		} finally {
			bar.release();
		}
	}
	
	/**
	 * Send an extension message and return the response. This is for advanced
	 * use only.
	 * 
	 * @param request
	 *            String
	 * @param requestData
	 *            byte[]
	 * @return SftpMessage
	 * @throws SshException
	 * @throws SftpStatusException
	 */
	public UnsignedInteger32 sendExtensionMessage(String request, byte[] requestData)
			throws SshException, SftpStatusException {

		try {
			UnsignedInteger32 id = nextRequestId();
			Packet packet = createPacket();
			packet.write(SSH_FXP_EXTENDED);
			packet.writeUINT32(id);
			packet.writeString(request);
			if(requestData!=null) {
				packet.write(requestData);
			}		
			sendMessage(packet);

			return id;
			
		} catch (IOException ex) {
			throw new SshException(SshException.INTERNAL_ERROR, ex);
		}
	}

	/**
	   * Get a packet from the available pool or create if non available
	   * @return Packet
	   * @throws IOException
	   */
	  protected Packet createPacket() throws IOException {
	    return PacketPool.getInstance().getPacket();
	  }
	  
	  class SftpThreadSynchronizer {

			boolean isBlocking = false;

			public boolean requestBlock(UnsignedInteger32 requestId,
					MessageHolder holder) throws InterruptedException {

				if (responses.containsKey(requestId)) {
					holder.msg = (Message) responses.get(requestId);
					return false;
				}

				synchronized (this) {
					boolean canBlock = !isBlocking;
					if (canBlock) {
						isBlocking = true;
					} else {
						wait();
					}
					return canBlock;
				}
			}

			public synchronized void releaseBlock() {
				isBlocking = false;
				notifyAll();
			}

		}

	public boolean isClosed() {
		return getSession().isClosed();
	}

	public UnsignedInteger32 getMaximumLocalWindowSize() {
		return getMaximumWindowSize();
	}

	public int getMaximumLocalPacketLength() {
		return getMaximumPacketSize();
	}

	public UnsignedInteger32 getMaximumRemoteWindowSize() {
		return session.getMaxiumRemoteWindowSize();
	}

	public int getMaximumRemotePacketLength() {
		return session.getMaxiumRemotePacketSize();
	}

	public SshClientContext getContext() {
		return (SshClientContext) con.getContext();
	}
}
