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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.sftp.extensions;

import com.sshtools.common.logger.Log;
import com.sshtools.common.sftp.SftpSpecification;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.util.ByteArrayReader;

public class MD5FileExtension extends AbstractMD5Extension {

	public static final String EXT_MD5_HASH = "md5-hash";
	
	public MD5FileExtension() {
		super(EXT_MD5_HASH);
	}

	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSpecification sftp) {

        try {
        	String filename = msg.readString();
	        long startOffset = msg.readUINT64().longValue();
	        long length = msg.readUINT64().longValue();
	        byte[] quickCheckHash = msg.readBinaryString();
	        byte[] hashValue;
	        
            hashValue = doMD5Hash(filename, startOffset, length, quickCheckHash, sftp);
            
            Packet reply = new Packet();
	       
            try {
            	reply.write(SSH_FXP_EXTENDED_REPLY);
            	reply.writeInt(requestId);
            	reply.writeString(extensionName);
            	reply.writeBinaryString(hashValue);
	       
            	sftp.sendMessage(reply);
	        
            } finally {
            	reply.close();
            }
	        
        } catch (Exception e) {
        	Log.error("Failed to process EXT_MD5_HASH", e);
            sftp.sendStatusMessage(requestId, SftpSpecification.STATUS_FX_FAILURE, e.getMessage());
            return;
        } 
	}
}
