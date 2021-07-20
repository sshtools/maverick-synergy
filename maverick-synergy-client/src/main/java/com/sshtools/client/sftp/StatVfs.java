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

import java.io.IOException;

import com.sshtools.common.util.UnsignedInteger64;

public class StatVfs {

	public static final int SSH_FXE_STATVFS_ST_RDONLY = 0x1; /* read-only */
	public static final int SSH_FXE_STATVFS_ST_NOSUID = 0x2; /* no setuid */

	UnsignedInteger64 f_bsize; /* file system block size */
	UnsignedInteger64 f_frsize; /* fundamental fs block size */
	UnsignedInteger64 f_blocks; /* number of blocks (unit f_frsize) */
	UnsignedInteger64 f_bfree; /* free blocks in file system */
	UnsignedInteger64 f_bavail; /* free blocks for non-root */
	UnsignedInteger64 f_files; /* total file inodes */
	UnsignedInteger64 f_ffree; /* free file inodes */
	UnsignedInteger64 f_favail; /* free file inodes for to non-root */
	UnsignedInteger64 f_fsid; /* file system id */
	UnsignedInteger64 f_flag; /* bit mask of f_flag values */
	UnsignedInteger64 f_namemax; /* maximum filename length */

	StatVfs(SftpMessage msg) throws IOException {

		f_bsize = msg.readUINT64();
		f_frsize = msg.readUINT64();
		f_blocks = msg.readUINT64();
		f_bfree = msg.readUINT64();
		f_bavail = msg.readUINT64();
		f_files = msg.readUINT64();
		f_ffree = msg.readUINT64();
		f_favail = msg.readUINT64();
		f_fsid = msg.readUINT64();
		f_flag = msg.readUINT64();
		f_namemax = msg.readUINT64();
	}

	public long getBlockSize() {
		return f_bsize.longValue();
	}

	public long getFragmentSize() {
		return f_frsize.longValue();
	}

	public long getBlocks() {
		return f_blocks.longValue();
	}

	public long getFreeBlocks() {
		return f_bfree.longValue();
	}

	public long getAvailBlocks() {
		return f_bavail.longValue();
	}

	public long getINodes() {
		return f_files.longValue();
	}

	public long getFreeINodes() {
		return f_ffree.longValue();
	}

	public long getAvailINodes() {
		return f_favail.longValue();
	}

	public long getFileSystemID() {
		return f_fsid.longValue();
	}

	public long getMountFlag() {
		return f_flag.longValue();
	}

	public long getMaximumFilenameLength() {
		return f_namemax.longValue();
	}

	public long getSize() {
		return getFragmentSize() * getBlocks() / 1024;
	}

	public long getUsed() {
		return getFragmentSize() * (getBlocks() - getFreeBlocks()) / 1024;
	}

	public long getAvailForNonRoot() {
		return getFragmentSize() * getAvailBlocks() / 1024;
	}

	public long getAvail() {
		return getFragmentSize() * getFreeBlocks() / 1024;
	}

	public int getCapacity() {
		return (int) (100 * (getBlocks() - getFreeBlocks()) / getBlocks());
	}

}
