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

public enum SftpSubsystemOperation {

	INIT,
	MAKE_DIRECTORY,
	RESOLVE_PATH,
	OPEN_FILE,
	READ_FILE,
	WRITE_FILE,
	OPEN_DIRECTORY,
	READ_DIRECTORY,
	REMOVE_DIRECTORY,
	CLOSE_HANDLE,
	REMOVE_FILE,
	RENAME_FILE,
	GET_ATTRIBUTES,
	SET_ATTRIBUTES,
	CREATE_SYMLINK,
	FOLLOW_SYMLINK,
	EXTENDED, 
	UNSUPPORTED;
}
