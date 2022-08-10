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

package com.sshtools.server.vsession;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.server.DefaultServerChannelFactory;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.ssh.ChannelNG;

public class VirtualChannelFactory extends DefaultServerChannelFactory {

	CommandFactory<? extends ShellCommand>[] factories;
	String shellCommand;
	@SafeVarargs
	public VirtualChannelFactory(CommandFactory<? extends ShellCommand>... factories) {
		this.factories = factories;
	}

	@SafeVarargs
	public VirtualChannelFactory(String shellCommand, CommandFactory<? extends ShellCommand>... factories) {
		this.factories = factories;
		this.shellCommand = shellCommand;
	}
	
	@Override
	protected ChannelNG<SshServerContext> createSessionChannel(SshConnection con)
			throws UnsupportedChannelException, PermissionDeniedException {
		return new VirtualShellNG(con,  new ShellCommandFactory(factories), shellCommand);
	}
	
	protected CommandFactory<? extends ShellCommand>[] getCommandFactories() {
		return factories;
	}
}
