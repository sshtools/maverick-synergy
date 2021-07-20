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

	public static final int SSH_FXP_STATUS = 101;
	public static final int SSH_FXP_HANDLE = 102;
	public static final int SSH_FXP_DATA = 103;
	public static final int SSH_FXP_NAME = 104;
	public static final int SSH_FXP_ATTRS = 105;

	public static final int SSH_FXP_EXTENDED = 200;
	public static final int SSH_FXP_EXTENDED_REPLY = 201;
	
	public AbstractFileSystem getFileSystem();
	public void sendStatusMessage(int requestId, int statusFxFailure, String message);
	public void sendMessage(Packet reply) throws IOException;
}
