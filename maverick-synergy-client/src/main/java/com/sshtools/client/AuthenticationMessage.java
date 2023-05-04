package com.sshtools.client;

import java.nio.ByteBuffer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.synergy.util.EncodingUtils;

public class AuthenticationMessage implements SshMessage {

	byte[] username;
	byte[] servicename;
	byte[] methodname;

	public AuthenticationMessage(String username, String servicename, String methodname) {
		this.username = EncodingUtils.getUTF8Bytes(username);
		this.servicename = EncodingUtils.getUTF8Bytes(servicename);
		this.methodname = EncodingUtils.getUTF8Bytes(methodname);
	}

	@Override
	public boolean writeMessageIntoBuffer(ByteBuffer buf) {

		buf.put((byte) AuthenticationProtocolClient.SSH_MSG_USERAUTH_REQUEST);
		buf.putInt(username.length);
		buf.put(username);
		buf.putInt(servicename.length);
		buf.put(servicename);
		buf.putInt(methodname.length);
		buf.put(methodname);

		return true;
	}
	
	@Override
	public void messageSent(Long sequenceNo) {

		if(Log.isDebugEnabled()) {
			Log.info("SSH_MSG_USERAUTH_REQUEST sent method="
					+ EncodingUtils.getUTF8String(methodname) + " username="
					+ EncodingUtils.getUTF8String(username));
		}
	}
}
