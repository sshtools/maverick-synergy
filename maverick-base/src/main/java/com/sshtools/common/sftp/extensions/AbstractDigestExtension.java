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
package com.sshtools.common.sftp.extensions;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.InvalidHandleException;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

public abstract class AbstractDigestExtension extends AbstractSftpExtension {

    static final Map<String,String> ALGOS;
    static {
		ALGOS = new HashMap<>();
		ALGOS.put("md5", JCEComponentManager.JCE_MD5);
		ALGOS.put("sha1", JCEComponentManager.JCE_SHA1);
		ALGOS.put("sha256", JCEComponentManager.JCE_SHA256);
		ALGOS.put("sha384", JCEComponentManager.JCE_SHA384);
		ALGOS.put("sha512", JCEComponentManager.JCE_SHA512);
	}
    
	AbstractDigestExtension(String extensionName) {
		super(extensionName, true);
	}

	protected byte[] doHash(String algorithm, String filename, long startOffset, long length,SftpSubsystem sftp) throws FileNotFoundException, PermissionDeniedException, IOException, SshException, InvalidHandleException {
		
		AbstractFileSystem fs = sftp.getFileSystem();
		byte[] handle = fs.openFile(filename, new UnsignedInteger32(AbstractFileSystem.OPEN_READ), null);
		try {
			return doHash(algorithm, handle, startOffset, length, sftp);
		} finally {
			fs.closeFile(handle);
			fs.freeHandle(handle);
		}
	}
	
	protected byte[] doHash(String algorithm, byte[] handle, long startOffset, long length, SftpSubsystem sftp) throws SshException, EOFException, InvalidHandleException, IOException, PermissionDeniedException {
		
		
		byte[] tmp = new byte[32768];
		AbstractFileSystem fs = sftp.getFileSystem();

		Digest digest = (Digest) JCEComponentManager.getInstance().supportedDigests().getInstance(ALGOS.get(algorithm));
		int read;
		long total = 0L;
		do {
			read = fs.readFile(handle, new UnsignedInteger64(startOffset), 
					tmp, 
					0, 
					length==0 ? tmp.length : Math.min(tmp.length, (int) (length - total)));
			if(read > 0) {
				digest.putBytes(tmp, 0, read);
				total += read;
				startOffset += read;
			} else {
				if(total == 0) {
					throw new EOFException();
				}
			}
		} while(read > -1 && (length==0 || total < length));
		return digest.doFinal();
	}
	
	@Override
	public boolean supportsExtendedMessage(int messageId) {
		return false;
	}

	@Override
	public void processExtendedMessage(ByteArrayReader msg, SftpSubsystem sftp) {
	}

}
