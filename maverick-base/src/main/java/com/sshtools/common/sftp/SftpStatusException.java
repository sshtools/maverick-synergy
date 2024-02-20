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

/**
 * This exception is thrown by the {@link SftpSubsystemChannel} class whenever a
 * status message is received that is not expected. This would normally indicate
 * a failure of the operation, for example with a no such file status being
 * returned.
 * 
 * @author Lee David Painter
 */
public class SftpStatusException extends Exception {

	private static final long serialVersionUID = 3611692615088253193L;
	/** Everything performed correctly **/
	public static final int SSH_FX_OK = 0;
	/** The file or listing is EOF **/
	public static final int SSH_FX_EOF = 1;
	/** No such file was found **/
	public static final int SSH_FX_NO_SUCH_FILE = 2;
	/** The user does not have permission **/
	public static final int SSH_FX_PERMISSION_DENIED = 3;
	/** Generic failure code **/
	public static final int SSH_FX_FAILURE = 4;
	/** The client sent a bad protocol message **/
	public static final int SSH_FX_BAD_MESSAGE = 5;
	/** There is no connection to the file system **/
	public static final int SSH_FX_NO_CONNECTION = 6;
	/** The file system connection was lost **/
	public static final int SSH_FX_CONNECTION_LOST = 7;
	/** The operation requested is not supported **/
	public static final int SSH_FX_OP_UNSUPPORTED = 8;
	/** The file handle provided was invalid **/
	public static final int SSH_FX_INVALID_HANDLE = 9;
	/** The path is invalid */
	public static final int SSH_FX_NO_SUCH_PATH = 10;
	/** The path already exists */
	public static final int SSH_FX_FILE_ALREADY_EXISTS = 11;
	/** Cannot write to remote location */
	public static final int SSH_FX_WRITE_PROTECT = 12;
	/** There is no media available at the remote location */
	public static final int SSH_FX_NO_MEDIA = 13;

	// These error codes are not part of the supported versions however are
	// included as some servers are returning them.
	public static final int SSH_FX_NO_SPACE_ON_FILESYSTEM = 14;
	public static final int SSH_FX_QUOTA_EXCEEDED = 15;
	public static final int SSH_FX_UNKNOWN_PRINCIPAL = 16;
	public static final int SSH_FX_LOCK_CONFLICT = 17;
	public static final int SSH_FX_DIR_NOT_EMPTY = 18;
	public static final int SSH_FX_NOT_A_DIRECTORY = 19;
	public static final int SSH_FX_INVALID_FILENAME = 20;
	public static final int SSH_FX_LINK_LOOP = 21;
	public static final int SSH_FX_CANNOT_DELETE = 22;
	public static final int SSH_FX_INVALID_PARAMETER = 23;
	public static final int SSH_FX_FILE_IS_A_DIRECTORY = 24;
	public static final int SSH_FX_BYTE_RANGE_LOCK_CONFLICT = 25;
	public static final int SSH_FX_BYTE_RANGE_LOCK_REFUSED = 26;
	public static final int SSH_FX_DELETE_PENDING = 27;
	public static final int SSH_FX_FILE_CORRUPT = 28;
	public static final int SSH_FX_OWNER_INVALID = 29;
	public static final int SSH_FX_GROUP_INVALID = 30;
	public static final int SSH_FX_NO_MATCHING_BYTE_RANGE_LOCK = 31;

	/** The file handle provided was invalid **/
	public static final int INVALID_HANDLE = 100;

	/** The file cannot be resumed **/
	public static final int INVALID_RESUME_STATE = 101;

	/**
	 * The server reported a canonical newline convention not supported by this
	 * API
	 */
	public static final int INVALID_TEXT_MODE = 102;

	public static final int ATTRIBUTE_BITS_NOT_AVAILABLE = 9999;
	
	public static final int BAD_API_USAGE = Integer.MAX_VALUE;
	
	
	int status;

	public SftpStatusException(int status, String msg) {
		super(getStatusText(status) + (getStatusText(status).equalsIgnoreCase(msg) ? "" : ": " + msg));
		this.status = status;
	}

	public SftpStatusException(int status) {
		this(status, getStatusText(status));
	}

	/**
	 * Get the status
	 * 
	 * @return int
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Convert a SSH_FXP_STATUS code into a readable string
	 * 
	 * @param status
	 * @return
	 */
	public static String getStatusText(int status) {
		switch (status) {
		case SSH_FX_OK:
			return "OK";
		case SSH_FX_EOF:
			return "EOF";
		case SSH_FX_NO_SUCH_FILE:
			return "No such file";
		case SSH_FX_PERMISSION_DENIED:
			return "Permission denied";
		case SSH_FX_FAILURE:
			return "Server responded with an unknown failure";
		case SSH_FX_BAD_MESSAGE:
			return "Server responded to a bad message";
		case SSH_FX_NO_CONNECTION:
			return "No connection available";
		case SSH_FX_CONNECTION_LOST:
			return "Connection lost";
		case SSH_FX_OP_UNSUPPORTED:
			return "The operation is unsupported";
		case SSH_FX_INVALID_HANDLE:
		case INVALID_HANDLE:
			return "Invalid file handle";
		case SSH_FX_NO_SUCH_PATH:
			return "No such path";
		case SSH_FX_FILE_ALREADY_EXISTS:
			return "File already exists";
		case SSH_FX_WRITE_PROTECT:
			return "Write protect error";
		case SSH_FX_NO_MEDIA:
			return "No media at location";
		case SSH_FX_NO_SPACE_ON_FILESYSTEM:
			return "No space on filesystem";
		case SSH_FX_QUOTA_EXCEEDED:
			return "Quota exceeded";
		case SSH_FX_UNKNOWN_PRINCIPAL:
			return "Unknown principal";
		case SSH_FX_LOCK_CONFLICT:
			return "Lock conflict";
		case SSH_FX_DIR_NOT_EMPTY:
			return "Dir not empty";
		case SSH_FX_NOT_A_DIRECTORY:
			return "Not a directory";
		case SSH_FX_INVALID_FILENAME:
			return "Invalid filename";
		case SSH_FX_LINK_LOOP:
			return "Link loop";
		case SSH_FX_CANNOT_DELETE:
			return "Cannot delete";
		case SSH_FX_INVALID_PARAMETER:
			return "Invalid parameter";
		case SSH_FX_FILE_IS_A_DIRECTORY:
			return "File is a directory";
		case SSH_FX_BYTE_RANGE_LOCK_CONFLICT:
			return "Byte range lock conflict";
		case SSH_FX_BYTE_RANGE_LOCK_REFUSED:
			return "Byte range lock refused";
		case SSH_FX_DELETE_PENDING:
			return "Delete pending";
		case SSH_FX_FILE_CORRUPT:
			return "File corrupt";
		case SSH_FX_OWNER_INVALID:
			return "Owner invalid";
		case SSH_FX_GROUP_INVALID:
			return "Group invalid";
		case SSH_FX_NO_MATCHING_BYTE_RANGE_LOCK:
			return "No matching byte range lock";
		case INVALID_RESUME_STATE:
			return "Invalid resume state";
		case ATTRIBUTE_BITS_NOT_AVAILABLE:
			return "Attribute bits field is not available. Did you check hasAttributeBits()?";
		case BAD_API_USAGE:
			return "Bad API usage";
		default:
			return "Unknown status type " + String.valueOf(status);
		}
	}

}
