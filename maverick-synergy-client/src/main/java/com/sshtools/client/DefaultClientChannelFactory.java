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
package com.sshtools.client;

import java.util.Map;

import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ChannelFactory;
import com.sshtools.common.ssh.ChannelNG;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SshConnection;
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
	public final ChannelNG<SshClientContext> createChannel(String channeltype, SshConnection con)
			throws UnsupportedChannelException, PermissionDeniedException {

		
		if(channeltype.equals(RemoteForwardingClientChannel.REMOTE_FORWARDING_CHANNEL_TYPE)) {
			return new RemoteForwardingClientChannel(con);
		}
		
		return onCreateChannel(channeltype, con);
	}

	protected ChannelNG<SshClientContext> onCreateChannel(String channeltype, SshConnection con) 
			throws UnsupportedChannelException, PermissionDeniedException {
		throw new UnsupportedChannelException(String.format("%s is not a supported channel type", channeltype));
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
