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
