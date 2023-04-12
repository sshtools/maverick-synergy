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

import java.util.Date;

import com.sshtools.common.util.UnsignedInteger32;

public class TransferEvent {
		byte[] handle;
		String path;
		AbstractFileSystem nfs;
		long bytesRead = 0;
		long bytesWritten = 0;
		boolean exists = false;
		boolean hasReachedEOF = false;
		UnsignedInteger32 flags;
		Date started = new Date();
		public boolean isDir;
		public boolean error = false;
		public Throwable ex;
		String key;
		public boolean forceClose;
		
		public byte[] getHandle() {
			return handle;
		}
		public void setHandle(byte[] handle) {
			this.handle = handle;
		}
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
		}
		public AbstractFileSystem getNfs() {
			return nfs;
		}
		public void setNfs(AbstractFileSystem nfs) {
			this.nfs = nfs;
		}
		public long getBytesRead() {
			return bytesRead;
		}
		public void setBytesRead(long bytesRead) {
			this.bytesRead = bytesRead;
		}
		public long getBytesWritten() {
			return bytesWritten;
		}
		public void setBytesWritten(long bytesWritten) {
			this.bytesWritten = bytesWritten;
		}
		public boolean isExists() {
			return exists;
		}
		public void setExists(boolean exists) {
			this.exists = exists;
		}
		public boolean isHasReachedEOF() {
			return hasReachedEOF;
		}
		public void setHasReachedEOF(boolean hasReachedEOF) {
			this.hasReachedEOF = hasReachedEOF;
		}
		public UnsignedInteger32 getFlags() {
			return flags;
		}
		public void setFlags(UnsignedInteger32 flags) {
			this.flags = flags;
		}
		public Date getStarted() {
			return started;
		}
		public void setStarted(Date started) {
			this.started = started;
		}
		public boolean isDir() {
			return isDir;
		}
		public void setDir(boolean isDir) {
			this.isDir = isDir;
		}
		public boolean isError() {
			return error;
		}
		public void setError(boolean error) {
			this.error = error;
		}
		public Throwable getEx() {
			return ex;
		}
		public void setEx(Throwable ex) {
			this.ex = ex;
		}
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public boolean isForceClose() {
			return forceClose;
		}
		public void setForceClose(boolean forceClose) {
			this.forceClose = forceClose;
		}
		
	}