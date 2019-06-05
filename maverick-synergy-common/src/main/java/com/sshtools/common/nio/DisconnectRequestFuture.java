package com.sshtools.common.nio;

import com.sshtools.common.ssh.AbstractRequestFuture;

public class DisconnectRequestFuture extends AbstractRequestFuture {

	public void disconnected() {
		super.done(true);
	}
}
