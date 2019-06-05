/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.sftp.extensions;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.InvalidHandleException;
import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpSpecification;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.MD5Digest;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

public abstract class AbstractMD5Extension implements SftpExtension {

	
	String extensionName; 
	
	AbstractMD5Extension(String extensionName) {
		this.extensionName = extensionName;
	}

	protected byte[] doMD5Hash(String filename, long startOffset, long length, byte[] quickCheckHash, SftpSpecification sftp) throws FileNotFoundException, PermissionDeniedException, IOException, SshException, InvalidHandleException {
		
		AbstractFileSystem fs = sftp.getFileSystem();
		byte[] handle = fs.openFile(filename, new UnsignedInteger32(AbstractFileSystem.OPEN_READ), null);
		try {
			return doMD5Hash(handle, startOffset, length, quickCheckHash, sftp);
		} finally {
			fs.closeFile(handle);
		}
	}
	
	protected byte[] doMD5Hash(byte[] handle, long startOffset, long length, byte[] quickCheckHash, SftpSpecification sftp) throws SshException, EOFException, InvalidHandleException, IOException {
		
		byte[] tmp = new byte[32768];
		AbstractFileSystem fs = sftp.getFileSystem();
		MD5Digest digest = (MD5Digest) JCEComponentManager.getInstance().supportedDigests().getInstance("MD5");
		while(length > 0) {
			int read = fs.readFile(handle, new UnsignedInteger64(startOffset), tmp, 0, tmp.length);
			if(read > 0) {
				digest.putBytes(tmp, 0, read);
				length -= read;
				startOffset += read;
			}
		}
		return digest.doFinal();
	}
	
	@Override
	public boolean supportsExtendedMessage(int messageId) {
		return false;
	}

	@Override
	public void processExtendedMessage(ByteArrayReader msg, SftpSpecification sftp) {
	}
}
