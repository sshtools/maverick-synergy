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
package com.sshtools.common.sftp.extensions;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.InvalidHandleException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.MD5Digest;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

public abstract class AbstractMD5Extension extends AbstractSftpExtension {

	AbstractMD5Extension(String extensionName) {
		super(extensionName, false);
	}

	protected byte[] doMD5Hash(String filename, long startOffset, long length, byte[] quickCheckHash, SftpSubsystem sftp) throws FileNotFoundException, PermissionDeniedException, IOException, SshException, InvalidHandleException {
		
		AbstractFileSystem fs = sftp.getFileSystem();
		byte[] handle = fs.openFile(filename, new UnsignedInteger32(AbstractFileSystem.OPEN_READ), null);
		try {
			return doMD5Hash(handle, startOffset, length, quickCheckHash, sftp);
		} finally {
			fs.closeFile(handle);
		}
	}
	
	protected byte[] doMD5Hash(byte[] handle, long startOffset, long length, byte[] quickCheckHash, SftpSubsystem sftp) throws SshException, EOFException, InvalidHandleException, IOException, PermissionDeniedException {
		
		byte[] tmp = new byte[32768];
		AbstractFileSystem fs = sftp.getFileSystem();
		
		SftpFileAttributes attrs = fs.getFileAttributes(handle);
		
		if(length==0) {
			length = attrs.getSize().longValue();
		}
		
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
	
	protected void sendReply(int requestId, byte[] hashValue, SftpSubsystem sftp) throws IOException {
		
        Packet reply = new Packet();
	       
        try {
        	reply.write(SftpSubsystem.SSH_FXP_EXTENDED_REPLY);
        	reply.writeInt(requestId);
        	reply.writeString("md5-hash");
        	reply.writeBinaryString(hashValue);
       
        	sftp.sendMessage(reply);
        
        } finally {
        	reply.close();
        }
		
	}
	
	@Override
	public boolean supportsExtendedMessage(int messageId) {
		return false;
	}

	@Override
	public void processExtendedMessage(ByteArrayReader msg, SftpSubsystem sftp) {
	}

}
