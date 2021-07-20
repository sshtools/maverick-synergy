
package com.sshtools.common.ssh;

import com.sshtools.common.util.ByteArrayWriter;

public class SessionChannelHelper {

	public static void sendExitStatus(Channel channel, int exitcode) {

		if (!channel.isClosed()) {
			channel.sendChannelRequest("exit-status", false,
					ByteArrayWriter.encodeInt(exitcode));
		}
		
	}
}
