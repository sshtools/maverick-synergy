package com.sshtools.common.ssh;

import java.nio.ByteBuffer;

import com.sshtools.common.sshd.SshMessage;

public abstract class ByteArrayMessage implements SshMessage {

	byte[] msg;
	
	public ByteArrayMessage(byte[] msg) {
		this.msg = msg;
	}

	@Override
	public boolean writeMessageIntoBuffer(ByteBuffer buf) {
		buf.put(msg);
		return true;
	}

}
