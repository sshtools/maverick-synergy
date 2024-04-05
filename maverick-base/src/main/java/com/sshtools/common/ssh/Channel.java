package com.sshtools.common.ssh;

import java.io.IOException;

import com.sshtools.common.util.UnsignedInteger32;

public interface Channel {

	UnsignedInteger32 getLocalWindow();

	UnsignedInteger32 getRemoteWindow();

	int getLocalPacket();

	void close();

	void sendData(byte[] array, int i, int size) throws IOException;

	void sendWindowAdjust(UnsignedInteger32 count);

	boolean isClosed();

	void addEventListener(ChannelEventListener listener);

	void removeEventListener(ChannelEventListener listener);

	void sendChannelRequest(String requestName, boolean wantReply, byte[] data);

	void sendChannelRequest(String type, boolean wantreply,
			byte[] requestdata, ChannelRequestFuture future);

	SshConnection getConnection();

	Context getContext();

	boolean isRemoteEOF();

	boolean isLocalEOF();

	String getChannelType();
}
