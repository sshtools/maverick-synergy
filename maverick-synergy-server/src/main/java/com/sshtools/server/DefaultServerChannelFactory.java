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

package com.sshtools.server;

import java.io.IOException;
import java.util.Map;

import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SessionChannelServer;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.synergy.ssh.ChannelFactory;
import com.sshtools.synergy.ssh.ChannelNG;

public class DefaultServerChannelFactory implements ChannelFactory<SshServerContext> {

	
	public static final String LOCAL_FORWARDING_CHANNEL_TYPE = "direct-tcpip";
	
	protected ComponentFactory<ExecutableCommand> commands = new ComponentFactory<>(null);
	
	public DefaultServerChannelFactory() {
	}

	public ComponentFactory<ExecutableCommand> supportedCommands() {
		return commands;
	}
	
	public final ChannelNG<SshServerContext> createChannel(String channeltype, SshConnection con)
			throws UnsupportedChannelException, PermissionDeniedException, ChannelOpenException {

		
		if (channeltype.equals("session")) {
			return createSessionChannel(con);
		}
		
		if(channeltype.equals(LOCAL_FORWARDING_CHANNEL_TYPE)) {
			return createLocalForwardingChannel(con);
		}
		
		return onCreateChannel(channeltype, con);
	}

	protected ChannelNG<SshServerContext> createLocalForwardingChannel(SshConnection con) {
		return new com.sshtools.synergy.ssh.LocalForwardingChannel<SshServerContext>(
				LOCAL_FORWARDING_CHANNEL_TYPE,
				con);
	}

	protected ChannelNG<SshServerContext> onCreateChannel(String channeltype, SshConnection con) 
			throws UnsupportedChannelException, PermissionDeniedException {
		throw new UnsupportedChannelException(String.format("%s is not a supported channel type", channeltype));
	}
	
	protected ChannelNG<SshServerContext> createSessionChannel(SshConnection con)
			throws UnsupportedChannelException, PermissionDeniedException, ChannelOpenException {
		return new UnsupportedSession(con);
	}

	@Override
	public Subsystem createSubsystem(String name, SessionChannel session)
			throws UnsupportedChannelException, PermissionDeniedException {
		if(name.equals("sftp")) {
			return createSftpSubsystem(session);
		} else if(name.equals("publickey") || name.equals("publickey@vandyke.com")) {
			return createPublicKeySubsystem(session);
		}

		throw new UnsupportedChannelException();
	}
	
	protected SftpSubsystem createSftpSubsystem(SessionChannel session) 
			throws UnsupportedChannelException, PermissionDeniedException {
		try {
			SftpSubsystem sftp = new SftpSubsystem();
			sftp.init(session, session.getConnection().getContext());
			return sftp;
		} catch (IOException e) {
			if(Log.isErrorEnabled())
				Log.error("Failed to create sftp subsystem", e);
		}
		throw new UnsupportedChannelException();
	}
	
	protected PublicKeySubsystem createPublicKeySubsystem(SessionChannel session) throws UnsupportedChannelException, PermissionDeniedException {
		
		try {
			PublicKeySubsystem subsystem = new PublicKeySubsystem();
			subsystem.init(session, session.getConnection().getContext());
			return subsystem;
		} catch (IOException e) {
			if(Log.isErrorEnabled())
				Log.error("Failed to create publickey subsystem", e);
		} 
		throw new UnsupportedChannelException();
	}

	@Override
	public ExecutableCommand executeCommand(SessionChannel sessionChannel, String[] args, Map<String, String> environment) throws PermissionDeniedException, UnsupportedChannelException {
		
		if(args.length==0) {
			throw new UnsupportedChannelException("No arguments provided");
		}
		
		try {
			ExecutableCommand process = commands.getInstance(args[0]);
			process.init((SessionChannelServer)sessionChannel);
			process.createProcess(args, environment);
			return process;
		} catch (SshException e) {	
		}
		
		throw new UnsupportedChannelException(String.format("Command %s not found", args[0]));
		
	}
}
