/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.sftp;

import java.io.IOException;

import com.sshtools.common.ssh.Packet;

public interface SftpSpecification {

	public static final int STATUS_FX_OK = 0;
	public static final int STATUS_FX_EOF = 1;
	public static final int STATUS_FX_NO_SUCH_FILE = 2;
	public static final int STATUS_FX_PERMISSION_DENIED = 3;
	public static final int STATUS_FX_FAILURE = 4;
	public static final int STATUS_FX_OP_UNSUPPORTED = 8; // I think this value is >
	
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
	
	
	// version 4
	public static final int SSH_FXP_INIT = 1;
	public static final int SSH_FXP_VERSION = 2;
	public static final int SSH_FXP_OPEN = 3;
	public static final int SSH_FXP_CLOSE = 4;
	public static final int SSH_FXP_READ = 5;
	public static final int SSH_FXP_WRITE = 6;

	public static final int SSH_FXP_LSTAT = 7;
	public static final int SSH_FXP_FSTAT = 8;
	public static final int SSH_FXP_SETSTAT = 9;
	public static final int SSH_FXP_FSETSTAT = 10;
	public static final int SSH_FXP_OPENDIR = 11;
	public static final int SSH_FXP_READDIR = 12;
	public static final int SSH_FXP_REMOVE = 13;
	public static final int SSH_FXP_MKDIR = 14;
	public static final int SSH_FXP_RMDIR = 15;
	public static final int SSH_FXP_REALPATH = 16;
	public static final int SSH_FXP_STAT = 17;
	public static final int SSH_FXP_RENAME = 18;
	public static final int SSH_FXP_READLINK = 19;
	public static final int SSH_FXP_SYMLINK = 20;
	public static final int SSH_FXP_LINK = 21;
	public static final int SSH_FXP_BLOCK = 22;
	public static final int SSH_FXP_UNBLOCK = 23;

	public static final int SSH_FXP_STATUS = 101;
	public static final int SSH_FXP_HANDLE = 102;
	public static final int SSH_FXP_DATA = 103;
	public static final int SSH_FXP_NAME = 104;
	public static final int SSH_FXP_ATTRS = 105;

	public static final int SSH_FXP_EXTENDED = 200;
	public static final int SSH_FXP_EXTENDED_REPLY = 201;

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
	
	public AbstractFileSystem getFileSystem();
	public void sendStatusMessage(int requestId, int statusFxFailure, String message);
	public void sendMessage(Packet reply) throws IOException;
}
