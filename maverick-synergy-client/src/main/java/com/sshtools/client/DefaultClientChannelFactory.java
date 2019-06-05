package com.sshtools.client;

import java.util.Map;

import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ChannelNG;
import com.sshtools.common.ssh.ChannelFactory;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.ssh.UnsupportedChannelException;

/**
 * Implements a default ChannelFactory for an SSH client.
 */
public class DefaultClientChannelFactory implements ChannelFactory<SshClientContext> {

	
	
	public DefaultClientChannelFactory() {
		
	}
	
	/**
	 * Called when a channel needs to be created.
	 */
	public final ChannelNG<SshClientContext> createChannel(String channeltype, Connection<SshClientContext> con)
			throws UnsupportedChannelException, PermissionDeniedException {
		
		if(channeltype.equals("session")) {
			return createSessionChannel((Connection<SshClientContext>)con);
		}
		
		if(channeltype.equals(RemoteForwardingClientChannel.REMOTE_FORWARDING_CHANNEL_TYPE)) {
			return new RemoteForwardingClientChannel(con, con.getContext());
		}
		
		throw new UnsupportedChannelException(String.format("%s is not a supported channel type", channeltype));
	}
	
	/**
	 * Creates the session channel.
	 * 
	 * @param con
	 * @return
	 */
	protected ChannelNG<SshClientContext> createSessionChannel(Connection<SshClientContext> con) {
		return new SessionChannelNG(con, con.getContext().getSessionMaxPacketSize(), 
				con.getContext().getSessionMaxWindowSize(),
				con.getContext().getSessionMaxWindowSize(),
				con.getContext().getSessionMinWindowSize());
	}
	
	protected ChannelNG<SshClientContext> createSessionChannel(Connection<SshClientContext> con, ChannelRequestFuture future) {
		return new SessionChannelNG(con, con.getContext().getSessionMaxPacketSize(), 
				con.getContext().getSessionMaxWindowSize(), 				
				con.getContext().getSessionMaxWindowSize(),
				con.getContext().getSessionMinWindowSize(),
				future);
	}

	/**
	 * Request to create a subsystem. This method throws an UnsupportedChannelException as we do not support the opening
	 * of subsystems on the client.
	 */
	@Override
	public Subsystem createSubsystem(String name, SessionChannel session)
			throws UnsupportedChannelException, PermissionDeniedException {
		throw new PermissionDeniedException("Client cannot start subsystems");
	}

	@Override
	public ExecutableCommand executeCommand(String[] args, Map<String, String> environment)
			throws PermissionDeniedException, UnsupportedChannelException {
		throw new PermissionDeniedException("Client cannot execute commands");
	}
}
