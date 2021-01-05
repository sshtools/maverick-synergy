/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
