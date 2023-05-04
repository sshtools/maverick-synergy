/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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