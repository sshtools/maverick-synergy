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
