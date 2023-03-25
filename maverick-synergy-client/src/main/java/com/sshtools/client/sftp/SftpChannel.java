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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
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
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.ssh.RequestFuture;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.IOUtils;
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
	
	boolean performVerification = false;
	
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
	
					Map<String, byte[]> extensions = new HashMap<String, byte[]>();
	
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

		return (SftpMessage) responses.remove(requestId);

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
	 */
	public void changePermissions(SftpFile file, int permissions)
			throws SftpStatusException, SshException {
		SftpFileAttributes attrs = new SftpFileAttributes(
				SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
				getCharsetEncoding());
		attrs.setPermissions(new UnsignedInteger32(permissions));
		setAttributes(file, attrs);
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
	 */
	public void changePermissions(String filename, int permissions)
			throws SftpStatusException, SshException {
		SftpFileAttributes attrs = new SftpFileAttributes(
				SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
				getCharsetEncoding());
		attrs.setPermissions(new UnsignedInteger32(permissions));
		setAttributes(filename, attrs);
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

		SftpFileAttributes attrs = new SftpFileAttributes(
				SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
				getCharsetEncoding());
		attrs.setPermissions(permissions);
		setAttributes(filename, attrs);

	}

	/**
	 * Verify that an OK status has been returned for a request id.
	 * 
	 * @param requestId
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void getOKRequestStatus(UnsignedInteger32 requestId)
			throws SftpStatusException, SshException {

		SftpMessage bar = getResponse(requestId);
		try {
			if (bar.getType() == SSH_FXP_STATUS) {
				int status = (int) bar.readInt();
				if (status == SftpStatusException.SSH_FX_OK) {
					return;
				}

				if (version >= 3) {
					String msg = bar.readString();
					throw new SftpStatusException(status, msg);
				}
				throw new SftpStatusException(status);

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
	 * Sets the attributes of a file.
	 * 
	 * @param path
	 *            the path to the file.
	 * @param attrs
	 *            the file attributes.
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void setAttributes(String path, SftpFileAttributes attrs)
			throws SftpStatusException, SshException {
		try {
			UnsignedInteger32 requestId = nextRequestId();

			Packet msg = createPacket();
			msg.write(SSH_FXP_SETSTAT);
			msg.writeInt(requestId.longValue());
			msg.writeString(path, CHARSET_ENCODING);
			msg.write(attrs.toByteArray(getVersion()));

			sendMessage(msg);

			getOKRequestStatus(requestId);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex, SshException.INTERNAL_ERROR);
		}
	}

	/**
	 * Sets the attributes of a file.
	 * 
	 * @param file
	 *            the file object.
	 * @param attrs
	 *            the new attributes.
	 * 
	 * @throws SshException
	 */
	public void setAttributes(SftpFile file, SftpFileAttributes attrs)
			throws SftpStatusException, SshException {
		if (!isValidHandle(file.getHandle())) {
			throw new SftpStatusException(SftpStatusException.INVALID_HANDLE,
					"The handle is not an open file handle!");
		}

		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_FSETSTAT);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(file.getHandle());
			msg.write(attrs.toByteArray(getVersion()));

			sendMessage(msg);

			getOKRequestStatus(requestId);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
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
	 */
	public UnsignedInteger32 postWriteRequest(byte[] handle, long position,
			byte[] data, int off, int len) throws SftpStatusException,
			SshException {

		if ((data.length - off) < len) {
			throw new IndexOutOfBoundsException("Incorrect data array size!");
		}

		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_WRITE);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			msg.writeUINT64(position);
			msg.writeBinaryString(data, off, len);

			sendMessage(msg);

			return requestId;
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
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

		getOKRequestStatus(postWriteRequest(handle, offset.longValue(), data,
				off, len));
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
	 */
	public void performOptimizedWrite(String filename, byte[] handle, int blocksize,
			int outstandingRequests, java.io.InputStream in, int buffersize,
			FileTransferProgress progress) throws SftpStatusException,
			SshException, TransferCancelledException {
		performOptimizedWrite(filename, handle, blocksize, outstandingRequests, in,
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
	 */
	public void performOptimizedWrite(String filename, byte[] handle, int blocksize,
			int outstandingRequests, java.io.InputStream in, int buffersize,
			FileTransferProgress progress, long position)
			throws SftpStatusException, SshException,
			TransferCancelledException {

		long started = System.currentTimeMillis();
		long transfered = position;
		
		try {
			if (blocksize > -1 && blocksize < 4096) {
				throw new SshException("Block size cannot be less than 4096",
						SshException.BAD_API_USAGE);
			}

			if (blocksize < 0 || blocksize > 65536) {
				blocksize = getSession().getMaximumRemotePacketLength() - 13;
			} else if(blocksize + 13 > getSession().getMaxiumRemotePacketSize()) {
				blocksize = getSession().getMaximumRemotePacketLength() - 13;
			}
			
			if(outstandingRequests < 0) {
				outstandingRequests = (int) (getSession().getMaxiumRemoteWindowSize().longValue() / blocksize);
			}
			
			System.setProperty("maverick.write.optimizedBlock", String.valueOf(blocksize));
			
			if(Log.isTraceEnabled()) {
				Log.trace("Performing optimized write length=" + in.available()
						+ " postion=" + position + " blocksize=" + blocksize
						+ " outstandingRequests=" + outstandingRequests);
			}
			
			if (position < 0)
				throw new SshException(
						"Position value must be greater than zero!",
						SshException.BAD_API_USAGE);

			if (position > 0) {
				if (progress != null)
					progress.progressed(position);
			}
			
			if (buffersize <= 0) {
				buffersize = blocksize;
			}

			byte[] buf = new byte[blocksize];

			
			int buffered = 0;

			buffered = in.read(buf);
			if(buffered != -1) {
			
				long time = System.currentTimeMillis();
				writeFile(handle, new UnsignedInteger64(position), buf, 0, buffered);
				time = System.currentTimeMillis() - time;
				
				System.setProperty("maverick.write.blockRoundtrip", String.valueOf(time));
				
				transfered += buffered;
	
				if (progress != null) {
					if (progress.isCancelled())
						throw new TransferCancelledException();
					progress.progressed(transfered);
				}
				
				Vector<UnsignedInteger32> requests = new Vector<UnsignedInteger32>();
				// BufferedInputStream is not in J2ME, whatever type of input stream
				// has been passed in can be used in conjunction with the abstract
				// InputStream class.
				in = new BufferedInputStream(in, buffersize);
	
				while (true) {
	
					buffered = in.read(buf);
					if (buffered == -1)
						break;
	
					requests.addElement(postWriteRequest(handle, transfered, buf,
							0, buffered));
	
					transfered += buffered;
	
					if (progress != null) {
	
						if (progress.isCancelled())
							throw new TransferCancelledException();
	
						progress.progressed(transfered);
					}
					
					if (requests.size() > outstandingRequests) {
						requestId = (UnsignedInteger32) requests.elementAt(0);
						requests.removeElementAt(0);
						getOKRequestStatus(requestId);
					}
	
				}

				while(requests.size() > 0) {
					getOKRequestStatus(requests.remove(0));
				}
			}

		} catch (IOException ex) {
			throw new TransferCancelledException();
		} catch (OutOfMemoryError ex) {
			throw new SshException(
					"Resource Shortage: try reducing the local file buffer size",
					SshException.BAD_API_USAGE);
		} finally {
			long finished = System.currentTimeMillis();
			long transferTime = finished - started;
			double seconds = transferTime > 1000 ? transferTime / 1000 : 1D;
			if(Log.isInfoEnabled()) {
				if(transfered > 0) {
					Log.info("Optimized write of {} to {} took {} seconds at {} per second",  IOUtils.toByteSize(transfered), filename, seconds, IOUtils.toByteSize(transfered / seconds, 1));
				} else {
					Log.info("Optimized write did not transfer any data");
				}
			}
		}

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
	 */
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
	 */
	public void performOptimizedRead(String filename, byte[] handle, long length, int blocksize,
			OutputStream out, int outstandingRequests,
			FileTransferProgress progress, long position)
			throws SftpStatusException, SshException,
			TransferCancelledException {

		long transfered = 0;
		boolean reachedEOF = false;
		long started = System.currentTimeMillis();
		
		if (blocksize > -1 && blocksize < 4096) {
			throw new SshException("Block size cannot be less than 4096",
					SshException.BAD_API_USAGE);
		}

		if (blocksize < 0 || blocksize > 65536) {
			blocksize = getSession().getMaximumLocalPacketLength() - 13;
		} else if(blocksize + 13 > getSession().getMaximumLocalPacketLength()) {
			blocksize = getSession().getMaximumLocalPacketLength() - 13;
		}
		
		if(outstandingRequests < 0) {
			outstandingRequests = (int) (getSession().getMaximumWindowSpace().longValue() / blocksize);
		}

		System.setProperty("maverick.read.optimizedBlock", String.valueOf(blocksize));
		
		if(Log.isTraceEnabled()) {
			Log.trace("Performing optimized read length=" + length
					+ " postion=" + position + " blocksize=" + blocksize
					+ " outstandingRequests=" + outstandingRequests);
		}
		
		if (length <= 0) {
			// We cannot perform an optimised read on this file since we don't
			// know its length so
			// here we assume its very large
			length = Long.MAX_VALUE;
		} 
		
		MessageDigest md5 = null;
		OutputStream originalStream = null;
		
		if(performVerification) {
			try {
				md5  = MessageDigest.getInstance("MD5");
			} catch(NoSuchAlgorithmException ex) {
				throw new SshException(ex);
			}
			originalStream = out;
			out = new DigestOutputStream(out, md5);
		}
		
		/**
		 * LDP - Obtain the first block using a synchronous call. We do this
		 * to determine if the server is conforming to the spec and
		 * returning as much data as we have asked for. If not we
		 * reconfigure the block size to the number of bytes returned.
		 */

		if (position < 0) {
			throw new SshException(
					"Position value must be greater than zero!",
					SshException.BAD_API_USAGE);
		}

		try {
			byte[] tmp = new byte[blocksize];
	
			long time = System.currentTimeMillis();
			int i = readFile(handle, new UnsignedInteger64(0), tmp, 0, tmp.length);
			time = System.currentTimeMillis() - time;
			
			System.setProperty("maverick.read.blockRoundtrip", String.valueOf(time));
	
			// if i=-1 then eof so return, maybe should throw exception on null
			// files?
			if (i == -1) {
				return;
			}
			// if the first block contains required data, write to the output
			// buffer,
			// write the portion of tmp needed to out
			// change position
			if (i > position) {
				try {
					out.write(tmp, (int) position, (int) (i - position));
				} catch (IOException e) {
					throw new TransferCancelledException();
				}
				length = length - (i - position);
				transfered += (i - position);
				if (progress != null) {
					progress.progressed(transfered);
				}
				position = i;
	
			}
	
			// if the first block contains the whole portion of the file to be
			// read, then return
			if ((position + length) <= i) {
				return;
			}
	
			// reconfigure the blocksize if necessary
			if (i < blocksize && length > i) {
				blocksize = i;
			}
	
			Vector<UnsignedInteger32> requests = new Vector<UnsignedInteger32>(
					outstandingRequests);
			
			int osr = 1;
			
			long offset = position;	
			UnsignedInteger32 requestId;
			int dataLen;
			
			while (true) {
				
				while(requests.size() < osr) {
					
					if(i > 0 && session.getRemoteWindow().longValue() < 29) {
						if (Log.isDebugEnabled())
							Log.debug("Deferring post requests due to lack of remote window");
						break;
					}
					if(Log.isTraceEnabled())
						Log.trace("Posting request for file offset " + offset);
		
					requests.addElement(postReadRequest(handle, offset, blocksize));
					offset += blocksize;
		
					if (progress != null && progress.isCancelled()) {
						throw new TransferCancelledException();
					}
				}
				
				requestId = (UnsignedInteger32) requests.elementAt(0);
				requests.removeElementAt(0);
				SftpMessage bar = getResponse(requestId);
				try {
					if (bar.getType() == SSH_FXP_DATA) {
						dataLen = (int) bar.readInt();
	
						if(Log.isTraceEnabled())
							Log.trace("Got " + dataLen + " bytes of data");
	
						try {
							out.write(bar.array(), bar.getPosition(), dataLen);
						} catch (IOException e) {
							throw new TransferCancelledException();
						}
						transfered += dataLen;
						if (progress != null) {
							progress.progressed(transfered);
						}
					} else if (bar.getType() == SSH_FXP_STATUS) {
						int status = (int) bar.readInt();
						if (status == SftpStatusException.SSH_FX_EOF) {
	
							if(Log.isTraceEnabled())
								Log.trace("Received file EOF");
							reachedEOF = true; // Hack for bad servers
							return;
						}
						if (version >= 3) {
							String desc = bar.readString();
	
							if(Log.isTraceEnabled())
								Log.trace("Received status " + desc);
	
							throw new SftpStatusException(status, desc);
						}
	
						if(Log.isTraceEnabled())
							Log.trace("Received status " + status);
	
						throw new SftpStatusException(status);
					} else {
						throw new SshException(
								"The server responded with an unexpected message",
								SshException.CHANNEL_FAILURE);
					}
				} catch(IOException ex) {
					throw new SshException(
							"Failed to read expected data from server response",
							SshException.CHANNEL_FAILURE);
				} finally {
					bar.release();
				}
				
				if(osr < outstandingRequests) {
					osr++;
				}
			}
			
			
		} finally {
			
			long finished = System.currentTimeMillis();
			long transferTime = finished - started;
			double seconds = transferTime > 1000 ? transferTime / 1000 : 1D;
			if(transfered > 0) {
				Log.info("Optimized read of {} from {} took seconds {} at {} per second", IOUtils.toByteSize(transfered), filename, seconds, IOUtils.toByteSize(transfered / seconds, 1));
			} else {
				Log.info("Optimized read did not transfer any data");
			}
			
			if(reachedEOF && performVerification && transfered > 0) {
				try {
					out.flush();
					out.close();
					try {
						originalStream.close();
					} catch(IOException e) { }
					
					byte[] digest = md5.digest();
					
					ByteArrayWriter baw = new ByteArrayWriter();
					
					try {
						baw.writeBinaryString(handle);
						baw.writeUINT64(0);
						baw.writeUINT64(transfered);
						baw.writeBinaryString(new byte[0]);
						
						SftpMessage reply = getExtensionResponse(sendExtensionMessage("md5-hash-handle", baw.toByteArray()));
					
						reply.readString();
						byte[] remoteDigest = reply.readBinaryString();
						
						if(!Arrays.areEqual(digest, remoteDigest)) {
							throw new SshException("Remote file digest does not match local digest", SshException.POSSIBLE_CORRUPT_FILE);
						}
					} finally {
						baw.close();
					}
				} catch (IOException e) {
					Log.error("Error processing remote digest", e);
				} catch(SftpStatusException e) {
					if(e.getStatus()==SftpStatusException.SSH_FX_OP_UNSUPPORTED) {
						performVerification = false;
					} else {
						Log.error("Could not verify file", e);
					}
				} catch(SshException e) {
					if(reachedEOF && e.getReason()==SshException.POSSIBLE_CORRUPT_FILE) {
						throw e;
					} else if(!reachedEOF) {
						throw e;
					}
				}
			}
		}
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
	 */
	public void performSynchronousRead(byte[] handle, int blocksize,
			OutputStream out, FileTransferProgress progress, long position)
			throws SftpStatusException, SshException,
			TransferCancelledException {

		if(Log.isTraceEnabled())
			Log.trace("Performing synchronous read postion=" + position
					+ " blocksize=" + blocksize);

		if (blocksize < 1 || blocksize > 65536) {
			blocksize = getSession().getMaximumRemotePacketLength() - 13;
		} else if(blocksize + 13 < getSession().getMaxiumRemotePacketSize()) {
			blocksize = getSession().getMaximumLocalPacketLength() - 13;
		}
		
		if(Log.isInfoEnabled()) {
			Log.info("Optimised block size will be {}", blocksize);
		}

		if (position < 0) {
			throw new SshException("Position value must be greater than zero!",
					SshException.BAD_API_USAGE);
		}

		byte[] tmp = new byte[blocksize];

		int read;
		UnsignedInteger64 offset = new UnsignedInteger64(position);

		if (position > 0) {
			if (progress != null)
				progress.progressed(position);
		}

		try {
			while ((read = readFile(handle, offset, tmp, 0, tmp.length)) > -1) {
				if (progress != null && progress.isCancelled()) {
					throw new TransferCancelledException();
				}
				out.write(tmp, 0, read);
				offset = UnsignedInteger64.add(offset, read);
				if (progress != null)
					progress.progressed(offset.longValue());
			}
		} catch (IOException e) {
			throw new SshException(e);
		}
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
	 */
	public UnsignedInteger32 postReadRequest(byte[] handle, long offset, int len)
			throws SftpStatusException, SshException {
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_READ);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			msg.writeUINT64(offset);
			msg.writeInt(len);

			if(Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_READ channel={} requestId={} offset={} blocksize={}",
				 						session.getLocalId(), requestId.toString(), offset, len);		
				 			}
			sendMessage(msg);

			return requestId;
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

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
	 */
	public int readFile(byte[] handle, UnsignedInteger64 offset, byte[] output,
			int off, int len) throws SftpStatusException, SshException {

		try {
			if ((output.length - off) < len) {
				throw new IndexOutOfBoundsException(
						"Output array size is smaller than read length!");
			}

			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_READ);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			msg.write(offset.toByteArray());
			msg.writeInt(len);

			sendMessage(msg);

			SftpMessage bar = getResponse(requestId);

			try {
				if (bar.getType() == SSH_FXP_DATA) {
					byte[] msgdata = bar.readBinaryString();
					System.arraycopy(msgdata, 0, output, off, msgdata.length);
					
					if(Log.isDebugEnabled()) {
						Log.debug("Received SSH_FXP_DATA channel={} requestId={} offset={} blocksize={}",
						 		session.getLocalId(), requestId.toString(), offset.toString(), msgdata.length);		
						 				}
					return msgdata.length;
				} else if (bar.getType() == SSH_FXP_STATUS) {
					int status = (int) bar.readInt();
					if (status == SftpStatusException.SSH_FX_EOF)
						return -1;
					if (version >= 3) {
						String desc = bar.readString();
						throw new SftpStatusException(status, desc);
					}
					throw new SftpStatusException(status);
				} else {
					close();
					throw new SshException(
							"The server responded with an unexpected message",
							SshException.CHANNEL_FAILURE);
				}
			} finally {
				bar.release();
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

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
		SftpFile file = new SftpFile(absolute, getAttributes(absolute));
		file.sftp = this;
		return file;
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

	
	public void lockFile(byte[] handle, long offset, long length, int lockFlags) throws SftpStatusException, SshException {
		
		if(version < 6) {
			throw new SftpStatusException(
					SftpStatusException.SSH_FX_OP_UNSUPPORTED,
					"Locks are not supported by the server SFTP version "
							+ String.valueOf(version));
		}
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_BLOCK);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			msg.writeUINT64(offset);
			msg.writeUINT64(length);
			msg.writeInt(lockFlags);
			
			sendMessage(msg);

			getOKRequestStatus(requestId);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}
	
	public void unlockFile(byte[] handle, long offset, long length) throws SftpStatusException, SshException {
		if(version < 6) {
			throw new SftpStatusException(
					SftpStatusException.SSH_FX_OP_UNSUPPORTED,
					"Locks are not supported by the server SFTP version "
							+ String.valueOf(version));
		}
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_UNBLOCK);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			msg.writeUINT64(offset);
			msg.writeUINT64(length);
			
			sendMessage(msg);

			getOKRequestStatus(requestId);
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
			msg.write(version >= 6 ? SSH_FXP_LINK : SSH_FXP_SYMLINK);
			msg.writeInt(requestId.longValue());
			msg.writeString(linkpath, CHARSET_ENCODING);
			msg.writeString(targetpath, CHARSET_ENCODING);
			if(version >= 6) {
				msg.writeBoolean(true);
			}

			sendMessage(msg);

			getOKRequestStatus(requestId);
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

			sendMessage(msg);

			getOKRequestStatus(requestId);
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

			sendMessage(msg);

			SftpMessage fileMsg = getResponse(requestId);
			try {
				SftpFile[] files = extractFiles(fileMsg, null);
				return files[0].getAbsolutePath();
			} finally {
				fileMsg.release();
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
			sendMessage(msg);

			return getSingleFileResponse(getResponse(requestId), "SSH_FXP_REALPATH").getAbsolutePath();
			
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
	public SftpFile getSingleFileResponse(SftpMessage bar, String messageName) throws SshException, SftpStatusException {
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

				return files[0];
			} else if (bar.getType() == SSH_FXP_STATUS) {
				int status = (int) bar.readInt();
				if (version >= 3) {
					String desc = bar.readString();
					throw new SftpStatusException(status, desc);
				}
				throw new SftpStatusException(status);
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
	 */
	public int listChildren(SftpFile file, List<SftpFile> children)
			throws SftpStatusException, SshException {
		if (file.isDirectory()) {
			if (!isValidHandle(file.getHandle())) {
				file = openDirectory(file.getAbsolutePath());
				if (!isValidHandle(file.getHandle())) {
					throw new SftpStatusException(
							SftpStatusException.SSH_FX_FAILURE,
							"Failed to open directory");
				}
			}
		} else {
			throw new SshException("Cannot list children for this file object",
					SshException.BAD_API_USAGE);
		}

		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_READDIR);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(file.getHandle());

			sendMessage(msg);
			
			if(Log.isDebugEnabled()) {
				Log.debug("Sending list children request");
			}
			SftpMessage bar = getResponse(requestId);
			
			try {
				if (bar.getType() == SSH_FXP_NAME) {
			
					if(Log.isDebugEnabled()) {
						Log.debug("Received results");
					}
					
					SftpFile[] files = extractFiles(bar, file.getAbsolutePath());
	
					if(Log.isDebugEnabled()) {
						Log.debug("THere are {} results in this packet", files.length);
					}
					
					for (int i = 0; i < files.length; i++) {
						children.add(files[i]);
					}
					return files.length;
				} else if (bar.getType() == SSH_FXP_STATUS) {
					int status = (int) bar.readInt();
	
					if(Log.isDebugEnabled()) {
						Log.debug("Received status {}", status);
					}
					if (status == SftpStatusException.SSH_FX_EOF) {
						return -1;
					}
	
					if (version >= 3) {
						String desc = bar.readString();
						throw new SftpStatusException(status, desc);
					}
					throw new SftpStatusException(status);
	
				} else {
					close();
					throw new SshException(
							"The server responded with an unexpected message",
							SshException.CHANNEL_FAILURE);
				}
			} finally {
				bar.release();
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
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

				files[i] = new SftpFile(parent != null ? parent + shortname
						: shortname, new SftpFileAttributes(bar, getVersion(), getCharsetEncoding()));
				files[i].longname = longname;

				// Work out username/group from long name
				if (longname != null && version <= 3) {
					try {
						StringTokenizer t = new StringTokenizer(longname);
						t.nextToken();
						t.nextToken();
						String username = t.nextToken();
						String group = t.nextToken();

						files[i].getAttributes().setUsername(username);
						files[i].getAttributes().setGroup(group);

					} catch (Exception e) {

					}

				}

				files[i].setSFTPSubsystem(this);
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
		SftpFile file;

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
						file.close();
					} catch (SshException ioe7) {
						makeDirectory(tmp);
					}

				} while (idx > -1);

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
	public SftpFile openFile(String absolutePath, int flags)
			throws SftpStatusException, SshException {
		return openFile(absolutePath, flags, new SftpFileAttributes(
				SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
				getCharsetEncoding()));
	}

	/**
	 * Open a file.
	 * 
	 * @param absolutePath
	 * @param flags
	 * @param attrs
	 * @return SftpFile
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public SftpFile openFile(String absolutePath, int flags,
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
			
			if((flags & OPEN_CREATE)==OPEN_CREATE) {
				if((flags & OPEN_TRUNCATE)==OPEN_TRUNCATE) {
					newFlags |= SSH_FXF_CREATE_TRUNCATE;
					if(Log.isTraceEnabled()) {
						Log.trace("OPEN_CREATE and OPEN_TRUNCATE present, adding SSH_FXF_CREATE_TRUNCATE");
					}
				} 
			} else {
				newFlags |= SSH_FXF_OPEN_EXISTING;
				if(Log.isTraceEnabled()) {
					Log.trace("OPEN_CREATE not present, adding SSH_FXF_OPEN_EXISTING");
				}
			}
			
			if((flags & OPEN_TEXT)==OPEN_TEXT) {
				newFlags |= SSH_FXF_ACCESS_TEXT_MODE;
				if(Log.isTraceEnabled()) {
					Log.trace("OPEN_TEXT present adding SSH_FXF_ACCESS_TEXT_MODE");
				}
			}
			
			return openFileVersion5(absolutePath, newFlags, accessFlags, attrs);
		} else {
			if (attrs == null) {
				attrs = new SftpFileAttributes(
						SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
						getCharsetEncoding());
			}

			try {
				UnsignedInteger32 requestId = nextRequestId();
				Packet msg = createPacket();
				msg.write(SSH_FXP_OPEN);
				msg.writeInt(requestId.longValue());
				msg.writeString(absolutePath, CHARSET_ENCODING);
				msg.writeInt(flags);
				msg.write(attrs.toByteArray(getVersion()));

				sendMessage(msg);

				byte[] handle = getHandleResponse(requestId);

				SftpFile file = new SftpFile(absolutePath, null);
				file.setHandle(handle);
				file.setSFTPSubsystem(this);

				EventServiceImplementation.getInstance().fireEvent(
						(new Event(this,
								EventCodes.EVENT_SFTP_FILE_OPENED, true))
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										file.getAbsolutePath()));
				return file;
			} catch (SshIOException ex) {
				throw ex.getRealException();
			} catch (IOException ex) {
				throw new SshException(ex);
			}
		}
	}

	public SftpFile openFileVersion5(String absolutePath, int flags,
			int accessFlags, SftpFileAttributes attrs)
			throws SftpStatusException, SshException {

		if (attrs == null) {
			attrs = new SftpFileAttributes(
					SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN,
					getCharsetEncoding());
		}

		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_OPEN);
			msg.writeInt(requestId.longValue());
			msg.writeString(absolutePath, CHARSET_ENCODING);
			msg.writeInt(accessFlags);
			msg.writeInt(flags);
			msg.write(attrs.toByteArray(getVersion()));

			sendMessage(msg);

			byte[] handle = getHandleResponse(requestId);

			SftpFile file = new SftpFile(absolutePath, null);
			file.setHandle(handle);
			file.setSFTPSubsystem(this);

			EventServiceImplementation.getInstance().fireEvent(
					(new Event(this, EventCodes.EVENT_SFTP_FILE_OPENED,
							true)).addAttribute(
							EventCodes.ATTRIBUTE_FILE_NAME,
							file.getAbsolutePath()));
			return file;
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
	public SftpFile openDirectory(String path) throws SftpStatusException,
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
			sendMessage(msg);

			byte[] handle = getHandleResponse(requestId);

			SftpFile file = new SftpFile(absolutePath, attrs);
			file.setHandle(handle);
			file.setSFTPSubsystem(this);

			return file;
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

	}

	void closeHandle(byte[] handle) throws SftpStatusException, SshException {
		if (!isValidHandle(handle)) {
			throw new SftpStatusException(SftpStatusException.INVALID_HANDLE,
					"The handle is invalid!");
		}

		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_CLOSE);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);

			sendMessage(msg);

			getOKRequestStatus(requestId);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}

	/**
	 * Close a file or directory.
	 * 
	 * @param file
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void closeFile(SftpFile file) throws SftpStatusException,
			SshException {

		if (file.getHandle() != null) {
			closeHandle(file.getHandle());
			EventServiceImplementation.getInstance().fireEvent(
					(new Event(this, EventCodes.EVENT_SFTP_FILE_CLOSED,
							true)).addAttribute(
							EventCodes.ATTRIBUTE_FILE_NAME,
							file.getAbsolutePath()));
			file.setHandle(null);
		}
	}

	boolean isValidHandle(byte[] handle) {
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

			sendMessage(msg);

			getOKRequestStatus(requestId);
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
	 * @param filename
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public void removeFile(String filename) throws SftpStatusException,
			SshException {
		try {
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_REMOVE);
			msg.writeInt(requestId.longValue());
			msg.writeString(filename, CHARSET_ENCODING);

			sendMessage(msg);

			getOKRequestStatus(requestId);
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
										filename));
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
			sendMessage(msg);

			getOKRequestStatus(requestId);
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
		return getAttributes(path, SSH_FXP_STAT);
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
		return getAttributes(path, SSH_FXP_LSTAT);
	}

	protected SftpFileAttributes getAttributes(String path, int messageId)
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

			sendMessage(msg);

			SftpMessage bar = getResponse(requestId);
			try {
				return extractAttributes(bar);
			} finally {
				bar.release();
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}

	SftpFileAttributes extractAttributes(SftpMessage bar)
			throws SftpStatusException, SshException {
		try {
			if (bar.getType() == SSH_FXP_ATTRS) {
				return new SftpFileAttributes(bar, getVersion(), getCharsetEncoding());
			} else if (bar.getType() == SSH_FXP_STATUS) {
				int status = (int) bar.readInt();

				// Only read the message string if the version is >= 3
				if (version >= 3) {
					String msg = bar.readString();
					throw new SftpStatusException(status, msg);
				}
				throw new SftpStatusException(status);
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

	/**
	 * Get the attributes of a file.
	 * 
	 * @param file
	 * @return SftpFileAttributes
	 * @throws SftpStatusException
	 *             , SshException
	 */
	public SftpFileAttributes getAttributes(SftpFile file)
			throws SftpStatusException, SshException {

		try {
			if (!isValidHandle(file.getHandle())) {
				return getAttributes(file.getAbsolutePath());
			}
			UnsignedInteger32 requestId = nextRequestId();
			Packet msg = createPacket();
			msg.write(SSH_FXP_FSTAT);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(file.getHandle());
			if (version > 3) {
				msg.writeInt(SftpFileAttributes.SSH_FILEXFER_ATTR_SIZE
						| SftpFileAttributes.SSH_FILEXFER_ATTR_PERMISSIONS
						| SftpFileAttributes.SSH_FILEXFER_ATTR_ACCESSTIME
						| SftpFileAttributes.SSH_FILEXFER_ATTR_CREATETIME
						| SftpFileAttributes.SSH_FILEXFER_ATTR_MODIFYTIME
						| SftpFileAttributes.SSH_FILEXFER_ATTR_ACL
						| SftpFileAttributes.SSH_FILEXFER_ATTR_OWNERGROUP
						| SftpFileAttributes.SSH_FILEXFER_ATTR_SUBSECOND_TIMES
						| SftpFileAttributes.SSH_FILEXFER_ATTR_EXTENDED);
			}
			sendMessage(msg);

			SftpMessage attrMessage = getResponse(requestId);
			try {
				return extractAttributes(attrMessage);
			} finally {
				attrMessage.release();
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

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
		makeDirectory(path, new SftpFileAttributes(
				SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY,
				getCharsetEncoding()));
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

			sendMessage(msg);

			getOKRequestStatus(requestId);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}
	
	public byte[] getHandleResponse(UnsignedInteger32 requestId)
			throws SftpStatusException, SshException {
		return getHandleResponse(getResponse(requestId));
	}
	
	public byte[] getHandleResponse(SftpMessage bar)
			throws SftpStatusException, SshException {

		try {
			if (bar.getType() == SSH_FXP_HANDLE) {
				return bar.readBinaryString();
			} else if (bar.getType() == SSH_FXP_STATUS) {
				int status = (int) bar.readInt();

				if (version >= 3) {
					String msg = bar.readString();
					throw new SftpStatusException(status, msg);
				}
				throw new SftpStatusException(status);
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

	SftpMessage getExtensionResponse(UnsignedInteger32 requestId)
			throws SftpStatusException, SshException {

		SftpMessage bar = getResponse(requestId);
		try {
			if (bar.getType() == SSH_FXP_EXTENDED_REPLY) {
				return bar;
			} else if (bar.getType() == SSH_FXP_STATUS) {
				int status = (int) bar.readInt();

				if (version >= 3) {
					String msg = bar.readString();
					throw new SftpStatusException(status, msg);
				}
				throw new SftpStatusException(status);
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
