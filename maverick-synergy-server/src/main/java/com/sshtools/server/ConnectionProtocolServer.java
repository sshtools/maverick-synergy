/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.server;

import java.io.IOException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ChannelNG;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionProtocol;
import com.sshtools.common.ssh.ConnectionStateListener;
import com.sshtools.common.ssh.ConnectionTaskWrapper;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

public class ConnectionProtocolServer extends ConnectionProtocol<SshServerContext> {

	
	
	TransportProtocolServer transport;

	public ConnectionProtocolServer(TransportProtocolServer transport, String username) {
		super(transport, username);
		this.transport = transport;
	}

	@Override
	protected boolean isClient() {
		return false;
	}
	
	@Override
	protected void onStart() {

		SshServerContext context = getContext();
		con = context.getConnectionManager().registerConnection(this);

		addTask(ExecutorOperationSupport.EVENTS, new ConnectionTaskWrapper(transport.getConnection(), new Runnable() {
			public void run() {
				for (ConnectionStateListener<SshServerContext> stateListener : getContext().getStateListeners()) {
					stateListener.connected(con);
				}
			}
		}));
		
	}

	@Override
	public SshServerContext getContext() {
		return transport.getContext();
	}

	@Override
	public TransportProtocolServer getTransport() {
		return transport;
	}

	@Override
	protected boolean processTCPIPForward(ByteArrayReader bar, ByteArrayWriter response) throws IOException {
		
		boolean success = false;
		String addressToBind = bar.readString();
		int portToBind = (int) bar.readInt();

		if (getContext().getForwardingPolicy().checkInterfacePermitted(
				transport.getConnection(), addressToBind, portToBind)) {

			success = true;
			if(Log.isDebugEnabled())
				Log.debug("Forwarding Policy has "
						+ (success ? "authorized" : "denied") + " "
						+ username + " remote forwarding access for "
						+ addressToBind + ":" + portToBind);
		}

		
		if (success) {
		
			success = getContext().getForwardingManager()!=null;
			
			if(success) {
				boolean responseRequired = portToBind == 0;

				try {
					portToBind = getContext().getForwardingManager().startListening(
							addressToBind, portToBind, con, RemoteForwardingFactoryImpl.INSTANCE);
					success = true;
				} catch (SshException e) {
					success = false;
				}

				if (responseRequired) {
					response.writeInt(portToBind);
				}
			}
		}
	
		return success;
	}

	@Override
	protected boolean processTCPIPCancel(ByteArrayReader bar, ByteArrayWriter msg) throws IOException {
		
		String addressToBind = bar.readString();
		int portToBind = (int) bar.readInt();

		boolean success = getContext().getForwardingManager().stopListening(
				addressToBind, portToBind, getContext().getRemoteForwardingCancelKillsTunnels(), con);
		msg.writeInt(portToBind);
		return success;
	}
	
	@Override
	public String getName() {
		return "ssh-connection";
	}
	
	@Override
	protected ChannelNG<SshServerContext> createChannel(String channeltype, Connection<SshServerContext> con)
			throws UnsupportedChannelException, PermissionDeniedException {
		return getContext().getChannelFactory().createChannel(channeltype, con);
	}

	@Override
	protected void onStop() {
		
	}
}