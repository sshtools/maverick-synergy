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
import java.util.Map;

import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.synergy.common.ssh.ChannelFactory;
import com.sshtools.synergy.common.ssh.ChannelNG;

public class DefaultServerChannelFactory implements ChannelFactory<SshServerContext> {

	
	public static final String LOCAL_FORWARDING_CHANNEL_TYPE = "direct-tcpip";
	
	protected ComponentFactory<ExecutableCommand> commands = new ComponentFactory<>(null);
	
	public DefaultServerChannelFactory() {
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
		return new com.sshtools.synergy.common.ssh.LocalForwardingChannel<SshServerContext>(
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
	public ExecutableCommand executeCommand(String[] args, Map<String, String> environment) throws PermissionDeniedException, UnsupportedChannelException {
		
		if(args.length==0) {
			throw new UnsupportedChannelException("No arguments provided");
		}
		
		try {
			ExecutableCommand process = commands.getInstance(args[0]);
			process.createProcess(args, environment);
			return process;
		} catch (SshException e) {	
		}
		
		throw new UnsupportedChannelException(String.format("Command %s not found", args[0]));
		
	}
}
