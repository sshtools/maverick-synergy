package com.sshtools.server;

import java.io.IOException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.ChannelNG;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.ConnectionStateListener;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;

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
				for (ConnectionStateListener stateListener : getContext().getStateListeners()) {
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
							addressToBind, portToBind, con, null, 0);
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
			throws UnsupportedChannelException, PermissionDeniedException, ChannelOpenException {
		return getContext().getChannelFactory().createChannel(channeltype, con);
	}

	@Override
	protected void onStop() {
		
	}
}
