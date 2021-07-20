
package com.sshtools.common.sftp.extensions;

import com.sshtools.common.logger.Log;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;

public class MD5FileExtension extends AbstractMD5Extension {

	public static final String EXTENSION_NAME = "md5-hash";
	
	public MD5FileExtension() {
		super(EXTENSION_NAME);
	}

	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSubsystem sftp) {

        try {
        	String filename = msg.readString();
	        long startOffset = msg.readUINT64().longValue();
	        long length = msg.readUINT64().longValue();
	        byte[] quickCheckHash = msg.readBinaryString();
	        byte[] hashValue;
	        
            hashValue = doMD5Hash(filename, startOffset, length, quickCheckHash, sftp);
            
            sendReply(requestId, hashValue, sftp);
	        
        } catch (Exception e) {
        	Log.error("Failed to process EXT_MD5_HASH", e);
            sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_FAILURE, e.getMessage());
            return;
        } 
	}
}
