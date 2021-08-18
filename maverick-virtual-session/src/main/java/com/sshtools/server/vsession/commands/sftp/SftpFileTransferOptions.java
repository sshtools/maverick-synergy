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

package com.sshtools.server.vsession.commands.sftp;

public class SftpFileTransferOptions {

	private static final String SFTP_PUT_OPTION_RESUME = "a";
	private static final String SFTP_PUT_OPTION_FSYNC = "f";
	private static final String SFTP_PUT_OPTION_PERMISSION_CAPS = "P";
	private static final String SFTP_PUT_OPTION_PERMISSION = "p";
	private static final String SFTP_PUT_OPTION_RECURSE = "r";

	private boolean resume;
	private boolean fsync;
	private boolean permissionCaps;
	private boolean permission;
	private boolean recurse;

	public boolean isResume() {
		return resume;
	}

	public void setResume(boolean resume) {
		this.resume = resume;
	}

	public boolean isFsync() {
		return fsync;
	}

	public void setFsync(boolean fsync) {
		this.fsync = fsync;
	}

	public boolean isPermissionCaps() {
		return permissionCaps;
	}

	public void setPermissionCaps(boolean permissionCaps) {
		this.permissionCaps = permissionCaps;
	}

	public boolean isPermission() {
		return permission;
	}

	public void setPermission(boolean permission) {
		this.permission = permission;
	}

	public boolean isRecurse() {
		return recurse;
	}

	public void setRecurse(boolean recurse) {
		this.recurse = recurse;
	}

	static SftpFileTransferOptions parse(String options) {
		String[] parts = options.split("");

		SftpFileTransferOptions sftpFileTransferOptions = new SftpFileTransferOptions();

		for (String part : parts) {
			switch (part) {
			case SFTP_PUT_OPTION_RESUME:
				sftpFileTransferOptions.setResume(true);
				break;

			case SFTP_PUT_OPTION_FSYNC:
				sftpFileTransferOptions.setFsync(true);
				break;

			case SFTP_PUT_OPTION_PERMISSION_CAPS:
				sftpFileTransferOptions.setPermissionCaps(true);
				break;

			case SFTP_PUT_OPTION_PERMISSION:
				sftpFileTransferOptions.setPermission(true);
				break;

			case SFTP_PUT_OPTION_RECURSE:
				sftpFileTransferOptions.setRecurse(true);
				break;
			default:
				break;
			}
		}

		return sftpFileTransferOptions;
	}
}
