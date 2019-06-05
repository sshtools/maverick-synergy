package com.sshtools.common.sftp.extensions;

import com.sshtools.common.logger.Log;
import com.sshtools.common.sftp.SftpSpecification;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.util.ByteArrayReader;

public class MD5HandleExtension extends AbstractMD5Extension {

	public static final String EXT_MD5_HASH_HANDLE = "md5-hash-handle";
	
	public MD5HandleExtension() {
		super(EXT_MD5_HASH_HANDLE);
	}

	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSpecification sftp) {

        try {
        	byte[] handle = msg.readBinaryString();
	        long startOffset = msg.readUINT64().longValue();
	        long length = msg.readUINT64().longValue();
	        byte[] quickCheckHash = msg.readBinaryString();
	        byte[] hashValue;
	        
            hashValue = doMD5Hash(handle, startOffset, length, quickCheckHash, sftp);
            
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
        	Log.error("Failed to process EXT_MD5_HASH_HANDLE", e);
            sftp.sendStatusMessage(requestId, SftpSpecification.STATUS_FX_FAILURE, e.getMessage());
            return;
        } 
	}
}
