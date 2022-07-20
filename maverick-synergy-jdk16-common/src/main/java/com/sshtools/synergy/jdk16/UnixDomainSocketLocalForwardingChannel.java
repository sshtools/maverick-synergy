/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.synergy.jdk16;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.LocalForwardingChannel;
import com.sshtools.synergy.ssh.SshContext;

public class UnixDomainSocketLocalForwardingChannel<T extends SshContext> extends LocalForwardingChannel<T> {

	public UnixDomainSocketLocalForwardingChannel(String channelType, SshConnection con) {
		super(channelType, con);
	}

	public UnixDomainSocketLocalForwardingChannel(String channelType, SshConnection con, String hostToConnect,
			SocketChannel socketChannel) {
		super(channelType, con, hostToConnect, 0, socketChannel);
	}

	@Override
	protected SocketChannel createSocketChannel() throws IOException {
		var socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
		socketChannel.configureBlocking(false);
		return socketChannel;
	}

	@Override
	protected byte[] createChannel() throws IOException {
		var baw = new ByteArrayWriter();
		try {
			baw.writeString(hostToConnect);
			baw.writeString(""); // Reserved
			baw.writeInt(0); // Reserved

			return baw.toByteArray();

		} finally {
			baw.close();
		}
	}

	@Override
	protected SocketAddress createSocketAddress() {
		return UnixDomainSocketAddress.of(hostToConnect);
	}

	@Override
	protected void readForwarding(ByteArrayReader bar) throws IOException {
		hostToConnect = bar.readString(); // socket path
		bar.readString(); // reserved
		bar.readInt(); // reserved
	}

}
