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
package com.sshtools.synergy.ssh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.ssh.components.ComponentFactory;

public class CompoundChannelFactory<C extends SshContext> implements ChannelFactory<C> {
	
	private final List<ChannelFactory<C>> factories = new ArrayList<>();

	public CompoundChannelFactory() {
	}
	
	public CompoundChannelFactory(@SuppressWarnings("unchecked") ChannelFactory<C>... factories) {
		this.factories.addAll(Arrays.asList(factories));
	}
	
	public CompoundChannelFactory(Collection<ChannelFactory<C>> factories) {
		this.factories.addAll(factories);
	}
	
	public void addFactory(ChannelFactory<C> factory) {
		factories.add(factory);
	}
	
	public void removeFactory(ChannelFactory<C> factory) {
		factories.remove(factory);
	}
	
	public List<ChannelFactory<C>> getFactories() {
		return Collections.unmodifiableList(factories);
	}

	@Override
	public ComponentFactory<ExecutableCommand> supportedCommands() {
		/* TODO: This is not great. We can't create a facade to this 
		 * method due to how it works. So the first factory in the
		 * list must be the one that provides support for commands.
		 */
		return factories.get(0).supportedCommands();
	}

	@Override
	public ChannelNG<C> createChannel(String channeltype, SshConnection con)
			throws UnsupportedChannelException, PermissionDeniedException, ChannelOpenException {
		for(var f : factories) {
			try {
				return f.createChannel(channeltype, con);
			}
			catch(UnsupportedChannelException uce) {
				//
			}
		}
		throw new UnsupportedChannelException(String.format("%s is not a supported channel type", channeltype));
	}

	@Override
	public Subsystem createSubsystem(String name, SessionChannel session)
			throws UnsupportedChannelException, PermissionDeniedException {
		for(var f : factories) {
			try {
				return f.createSubsystem(name, session);
			}
			catch(UnsupportedChannelException uce) {
				//
			}
		}
		throw new UnsupportedChannelException(String.format("%s is not a supported subsystem", name));
	}

	@Override
	public ExecutableCommand executeCommand(SessionChannel channel, String[] args, Map<String, String> environment)
			throws PermissionDeniedException, UnsupportedChannelException {
		for(var f : factories) {
			try {
				return f.executeCommand(channel, args, environment);
			}
			catch(UnsupportedChannelException uce) {
				//
			}
		}
		throw new UnsupportedChannelException(String.format("%s is not a supported command", args[0]));
	}

}
