package com.sshtools.common.sftp.extensions;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.sshtools.common.logger.Log;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;

public class MD5HandleExtension extends AbstractMD5Extension {

	public static final String EXTENSION_NAME = "md5-hash-handle";
	
	public MD5HandleExtension() {
		super(EXTENSION_NAME);
	}

	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSubsystem sftp) {

        try {
        	byte[] handle = msg.readBinaryString();
	        long startOffset = msg.readUINT64().longValue();
	        long length = msg.readUINT64().longValue();
	        byte[] quickCheckHash = msg.readBinaryString();
	        byte[] hashValue;
	        
	        
            hashValue = doMD5Hash(handle, startOffset, length, quickCheckHash, sftp);
            
            sendReply(requestId, hashValue, sftp);
	        
        } catch (Exception e) {
        	Log.error("Failed to process EXT_MD5_HASH_HANDLE", e);
            sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_FAILURE, e.getMessage());
            return;
        } 
	}

}
