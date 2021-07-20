
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
