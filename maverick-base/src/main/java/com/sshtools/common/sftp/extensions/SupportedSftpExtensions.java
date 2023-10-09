package com.sshtools.common.sftp.extensions;

/**
 * Deprecated. Use {@link SftpExtensionLoaderFactory} and {@link BasicSftpExtensionFactory}.
 */
@Deprecated(since = "3.1.0", forRemoval = true)
public enum SupportedSftpExtensions {

	MD5_FILE_HASH,
	POSIX_RENAME,
	COPY_FILE,
	OPEN_DIRECTORY_WITH_FILTER,
	COPY_DATA,
	CHECK_FILE_NAME,
	CHECK_FILE_HANDLE,
	CREATE_MULTIPART_FILE,
	OPEN_PART_FILE,
	HARDLINK,
	STATVFS;
}
