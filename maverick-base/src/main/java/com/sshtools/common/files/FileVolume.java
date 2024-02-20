package com.sshtools.common.files;

public interface FileVolume {
	long blockSize();

	long underlyingBlockSize();

	long blocks();

	long freeBlocks();

	long userFreeBlocks();

	long totalInodes();

	long freeInodes();

	long userFreeInodes();

	long id();

	long flags();

	long maxFilenameLength();
}
