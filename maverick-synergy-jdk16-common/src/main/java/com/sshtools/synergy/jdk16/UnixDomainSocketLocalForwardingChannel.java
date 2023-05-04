/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
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
