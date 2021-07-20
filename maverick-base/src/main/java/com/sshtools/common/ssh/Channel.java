
package com.sshtools.common.ssh;

import java.io.IOException;

public interface Channel {

	int getLocalWindow();

	int getRemoteWindow();

	int getLocalPacket();

	void close();

	void sendData(byte[] array, int i, int size) throws IOException;

	void sendWindowAdjust(int bytesSinceLastWindowIssue);

	boolean isClosed();

	void addEventListener(ChannelEventListener listener);

	void sendChannelRequest(String requestName, boolean wantReply, byte[] data);

	void sendChannelRequest(String type, boolean wantreply,
			byte[] requestdata, ChannelRequestFuture future);

	SshConnection getConnection();

	Context getContext();

	boolean isRemoteEOF();

	boolean isLocalEOF();
}
