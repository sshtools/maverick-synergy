package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;

import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.forwarding.ForwardingRequest.ForwardingRole;
import com.sshtools.common.forwarding.ForwardingRequest.ForwardingType;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

public class UnixDomainSocketLocalForwardingChannel<T extends SshContext> extends AbstractLocalSocketForwardingChannel<T> implements UnixDomainSocketForwardingChannel {

	private String socketPath;

	public UnixDomainSocketLocalForwardingChannel(String channelType, SshConnection con) {
		super(channelType, con);
	}

	public UnixDomainSocketLocalForwardingChannel(String channelType, SshConnection con, String socketPath,
			SocketChannel socketChannel) {
		super(channelType, con);
		this.socketPath = socketPath;
		this.socketChannel = socketChannel;
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
			baw.writeString(socketPath);
			baw.writeString(""); // Reserved
			baw.writeInt(0); // Reserved

			return baw.toByteArray();

		} finally {
			baw.close();
		}
	}

	@Override
	protected boolean checkPermissions() {
		return getContext().getForwardingPolicy().validate(
				getConnectionProtocol().getTransport().getConnection(), 
				ForwardingRole.CONNECT,
				ForwardingType.LOCAL, ForwardingRequest.ofDomainSocketDestination(
						socketPath)
				);
	}

	@Override
	protected SocketAddress createSocketAddress() {
		return UnixDomainSocketAddress.of(socketPath);
	}

	@Override
	protected void readForwarding(ByteArrayReader bar) throws IOException {
		socketPath = bar.readString(); // socket path
		bar.readString(); // reserved
		bar.readInt(); // reserved
	}

	@Override
	protected String targetToConnect() {
		return socketPath;
	}

	@Override
	public String getPath() {
		return socketPath;
	}

}
