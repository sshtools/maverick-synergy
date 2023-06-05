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
 * Copyright (C) 2002-2023 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.synergy.niofs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.StatVfs;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.synergy.niofs.SftpFileAttributeViews.ExtendedSftpFileAttributeView;

final class SftpFileStore extends FileStore {
	private final String path;
	private final SftpClient sftp;
	
	SftpFileStore(SftpClient sftp, String path) {
		this.path = path;
		this.sftp = sftp;
	}

	@Override
	public String type() {
		return "remote";
	}

	@Override
	public boolean supportsFileAttributeView(String name) {
		return name.equals("basic") || name.equals("sftp") || name.equals("posix");
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		return type.equals(BasicFileAttributeView.class) || type.equals(ExtendedSftpFileAttributeView.class) || type.equals(PosixFileAttributeView.class);
	}

	@Override
	public String name() {
		return String.valueOf("vol-" + statVFS().getFileSystemID());
	}

	@Override
	public long getBlockSize() throws IOException {
		return statVFS().getBlockSize();
	}

	@Override
	public boolean isReadOnly() {
		return ( statVFS().getMountFlag() & StatVfs.SSH_FXE_STATVFS_ST_RDONLY ) != 0;
	}

	@Override
	public long getUsableSpace() throws IOException {
		var statVFS = statVFS();
		return statVFS.getFragmentSize() * (statVFS.getBlocks() - statVFS.getFreeBlocks());
	}

	@Override
	public long getUnallocatedSpace() throws IOException {
		return getUsableSpace();
	}

	@Override
	public long getTotalSpace() throws IOException {
		var svfs = statVFS();
		return svfs.getFragmentSize() * svfs.getBlocks();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
		if(type.equals(SftpFileStoreAttributeView.class))
			return (V)new SftpFileStoreAttributeView(statVFS());
		else
			return null;
	}

	@Override
	public Object getAttribute(String attribute) throws IOException {
		if(attribute.equals("sftp:readOnly")) {
			return isReadOnly();
		}
		else if(attribute.equals("sftp:noSuid")) {
			return ( statVFS().getMountFlag() & StatVfs.SSH_FXE_STATVFS_ST_NOSUID ) != 0;
		}
		else if(attribute.equals("sftp:blockSize")) {
			return statVFS().getBlockSize();
		}
		else if(attribute.equals("sftp:fragmentSize")) {
			return statVFS().getFragmentSize();
		}
		else if(attribute.equals("sftp:blocks")) {
			return statVFS().getBlocks();
		}
		else if(attribute.equals("sftp:freeBlocks")) {
			return statVFS().getFreeBlocks();
		}
		else if(attribute.equals("sftp:availBlocks")) {
			return statVFS().getAvailBlocks();
		}
		else if(attribute.equals("sftp:iNodes")) {
			return statVFS().getINodes();
		}
		else if(attribute.equals("sftp:freeINodes")) {
			return statVFS().getFreeINodes();
		}
		else if(attribute.equals("sftp:availINodes")) {
			return statVFS().getAvailINodes();
		}
		else if(attribute.equals("sftp:fileSystemId")) {
			return statVFS().getFileSystemID();
		}
		else if(attribute.equals("sftp:mountFlag")) {
			return statVFS().getMountFlag();
		}
		else if(attribute.equals("sftp:maximumFilenameLength")) {
			return statVFS().getMaximumFilenameLength();
		}
		else if(attribute.equals("sftp:size")) {
			return statVFS().getSize();
		}
		else if(attribute.equals("sftp:used")) {
			return statVFS().getUsed();
		}
		else if(attribute.equals("sftp:avail")) {
			return statVFS().getAvail();
		}
		else if(attribute.equals("sftp:availForNonRoot")) {
			return statVFS().getAvailForNonRoot();
		}
		else if(attribute.equals("sftp:capacity")) {
			return statVFS().getCapacity();
		}
		return null;
	}

	private StatVfs statVFS() {
    	try {
    		return sftp.statVFS(path);
		} catch (SshException | SftpStatusException e) {
			throw new UncheckedIOException(new IOException("Failed to get file store status.", e));
		}
	}
	
	public final static class SftpFileStoreAttributeView implements FileStoreAttributeView {
		private StatVfs statVfs;
		
		SftpFileStoreAttributeView(StatVfs statVfs) {
			this.statVfs = statVfs;
		}

		@Override
		public String name() {
			return "sftp";
		}

		public long blockSize() {
			return statVfs.getBlockSize();
		}

		public long fragmentSize() {
			return statVfs.getFragmentSize();
		}

		public long blocks() {
			return statVfs.getBlocks();
		}

		public long freeBlocks() {
			return statVfs.getFreeBlocks();
		}

		public long availBlocks() {
			return statVfs.getAvailBlocks();
		}

		public long iNodes() {
			return statVfs.getINodes();
		}

		public long freeINodes() {
			return statVfs.getFreeINodes();
		}

		public long availINodes() {
			return statVfs.getAvailINodes();
		}

		public long fileSystemID() {
			return statVfs.getFileSystemID();
		}

		public long mountFlag() {
			return statVfs.getMountFlag();
		}

		public long maximumFilenameLength() {
			return statVfs.getMaximumFilenameLength();
		}

		public long size() {
			return statVfs.getSize();
		}

		public long used() {
			return statVfs.getUsed();
		}

		public long availForNonRoot() {
			return statVfs.getAvailForNonRoot();
		}

		public long avail() {
			return statVfs.getAvail();
		}

		public int capacity() {
			return statVfs.getCapacity();
		}

		@Override
		public String toString() {
			return "SftpFileStoreAttributeView [name()=" + name() + ", blockSize()=" + blockSize() + ", fragmentSize()="
					+ fragmentSize() + ", blocks()=" + blocks() + ", freeBlocks()=" + freeBlocks() + ", availBlocks()="
					+ availBlocks() + ", iNodes()=" + iNodes() + ", freeINodes()=" + freeINodes() + ", availINodes()="
					+ availINodes() + ", fileSystemID()=" + fileSystemID() + ", mountFlag()=" + mountFlag()
					+ ", maximumFilenameLength()=" + maximumFilenameLength() + ", size()=" + size() + ", used()="
					+ used() + ", availForNonRoot()=" + availForNonRoot() + ", avail()=" + avail() + ", capacity()="
					+ capacity() + "]";
		}
	}
}