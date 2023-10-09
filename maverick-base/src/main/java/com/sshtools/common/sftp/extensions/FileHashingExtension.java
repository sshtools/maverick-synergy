package com.sshtools.common.sftp.extensions;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.util.ByteArrayReader;

public abstract class FileHashingExtension extends AbstractDigestExtension {

	
	public FileHashingExtension(String name) {
		super(name);
	}

	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSubsystem sftp) {

        try {
        	byte[] handle = getFileHandle(msg, sftp);
        	String algorithms = msg.readString();
	        long startOffset = msg.readUINT64().longValue();
	        long length = msg.readUINT64().longValue();
	        int blockSize = (int) msg.readInt();
	        
	        String selectedAlgorithm = selectAlgorithm(algorithms);
	        
	        if(blockSize <= 1) {
	            byte[] hashValue = doHash(selectedAlgorithm, handle, startOffset, length, sftp);
	            sendReply(requestId, selectedAlgorithm, Arrays.asList(hashValue), sftp);
	        } else {
	        	List<byte[]> hashes = new ArrayList<>();
	        	while(true) {
	        		try {
	        			hashes.add(doHash(selectedAlgorithm, handle, startOffset, blockSize, sftp));
	        			startOffset += blockSize;
	        		} catch(EOFException e ) {
	        			break;
	        		}
	        	}
	        	sendReply(requestId, selectedAlgorithm, hashes, sftp);
	        }
	        
        } catch (Exception e) {
        	Log.error("Failed to process EXT_MD5_HASH_HANDLE", e);
            sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_FAILURE, e.getMessage());
            return;
        } 
	}
	
	protected abstract byte[] getFileHandle(ByteArrayReader msg, SftpSubsystem sftp) throws IOException, PermissionDeniedException;

	protected void sendReply(int requestId, String algorithm, List<byte[]> hashValue, SftpSubsystem sftp) throws IOException {
		
        Packet reply = new Packet();
	       
        try {
        	reply.write(SftpSubsystem.SSH_FXP_EXTENDED_REPLY);
        	reply.writeInt(requestId);
        	reply.writeString(algorithm);
        	for(byte[] hash : hashValue) {
        		reply.write(hash);
        	}
       
        	sftp.sendMessage(reply);
        
        } finally {
        	reply.close();
        }
		
	}

	private String selectAlgorithm(String algorithms) {
		String[] tmp = algorithms.split(",");
		for(String alg : tmp) {
			String result = ALGOS.get(alg);
			if(Objects.nonNull(result)) {
				return alg;
			}
		}
		return null;
	}
}
