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

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.InvalidHandleException;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.ssh.SshException;

public abstract class AbstractMD5Extension extends AbstractDigestExtension {

	AbstractMD5Extension(String extensionName) {
		super(extensionName);
	}
	
	protected byte[] doMD5Hash(String filename, long startOffset, long length, byte[] quickCheckHash, SftpSubsystem sftp) throws FileNotFoundException, PermissionDeniedException, IOException, SshException, InvalidHandleException {
		
		// What to do with quick check hash?
		return doHash("md5", filename, startOffset, length, sftp);
	}
	
	protected byte[] doMD5Hash(byte[] handle, long startOffset, long length, byte[] quickCheckHash, SftpSubsystem sftp) throws SshException, EOFException, InvalidHandleException, IOException, PermissionDeniedException {
		
		// What to do with quick check hash?
		return doHash("md5", handle, startOffset, length, sftp);
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

}
