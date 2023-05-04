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
package com.sshtools.server.jdk16;

import java.io.IOException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.jdk16.UnixDomainSockets;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.GlobalRequestHandler;

public class UnixDomainSocketGlobalRequestHandler implements GlobalRequestHandler<SshServerContext> {

	@Override
	public boolean processGlobalRequest(GlobalRequest request, ConnectionProtocol<SshServerContext> connection,
			boolean wantreply, ByteArrayWriter response) throws GlobalRequestHandlerException, IOException {
		if (UnixDomainSockets.STREAM_LOCAL_FORWARD_REQUEST.equals(request.getName())) {
			boolean success = false;
			var bar = new ByteArrayReader(request.getData());
			var path = bar.readString();

			if (connection.getContext().getForwardingPolicy().checkInterfacePermitted(connection.getConnection(), path,
					0)) {
				success = true;
				if (Log.isDebugEnabled())
					Log.debug("Forwarding Policy has " + (success ? "authorized" : "denied") + " "
							+ connection.getUsername() + " remote domain socket forwarding access for " + path);
			}

			if (success) {
				success = connection.getContext().getForwardingManager() != null;
				if (success) {
					try {
						connection.getContext().getForwardingManager().startListening(path, 0,
								connection.getConnection(), null, 0);
						return true;
					} catch (SshException e) {
					}
				}
			}
			return false;
		} else if (UnixDomainSockets.CANCEL_STREAM_LOCAL_FORWARD_REQUEST.equals(request.getName())) {
			var bar = new ByteArrayReader(request.getData());
			var path = bar.readString();
			if (connection.getContext().getForwardingManager().stopListening(path, 0,
					connection.getContext().getRemoteForwardingCancelKillsTunnels(), connection.getConnection())) {
				return true;
			}
			return false;
		}
		throw new GlobalRequestHandlerException();
	}

	@Override
	public String[] supportedRequests() {
		return new String[] { UnixDomainSockets.STREAM_LOCAL_FORWARD_REQUEST,
				UnixDomainSockets.CANCEL_STREAM_LOCAL_FORWARD_REQUEST };
	}

}
