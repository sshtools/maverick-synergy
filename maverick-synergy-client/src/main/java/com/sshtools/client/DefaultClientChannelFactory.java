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

package com.sshtools.client;

import java.util.Map;

import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.synergy.ssh.ChannelFactory;
import com.sshtools.synergy.ssh.ChannelNG;

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
	public ExecutableCommand executeCommand(SessionChannel channel, String[] args, Map<String, String> environment)
			throws PermissionDeniedException, UnsupportedChannelException {
		throw new PermissionDeniedException("Client cannot execute commands");
	}

	@Override
	public ComponentFactory<ExecutableCommand> supportedCommands() {
		throw new UnsupportedOperationException("Commands are not supported in client configurations");
	}
}
