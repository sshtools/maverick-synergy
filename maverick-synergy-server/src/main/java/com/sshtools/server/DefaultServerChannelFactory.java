package com.sshtools.server;

import java.io.IOException;
import java.util.Map;

import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.ssh.ChannelNG;
import com.sshtools.common.ssh.ChannelFactory;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.ssh.components.ComponentFactory;

public class DefaultServerChannelFactory implements ChannelFactory<SshServerContext> {

	
	public static final String LOCAL_FORWARDING_CHANNEL_TYPE = "direct-tcpip";
	
	protected ComponentFactory<ExecutableCommand> commands = new ComponentFactory<>(null);
	
	public DefaultServerChannelFactory() {
	}

	public final ChannelNG<SshServerContext> createChannel(String channeltype, Connection<SshServerContext> con)
			throws UnsupportedChannelException, PermissionDeniedException {

		
		if (channeltype.equals("session")) {
			return createSessionChannel(con);
		}
		
		if(channeltype.equals(LOCAL_FORWARDING_CHANNEL_TYPE)) {
			return new com.sshtools.common.ssh.LocalForwardingChannel<SshServerContext>(
					LOCAL_FORWARDING_CHANNEL_TYPE,
					con,
					con.getContext());
		}
		
		throw new UnsupportedChannelException(String.format("%s is not a supported channel type", channeltype));
	}

	protected ChannelNG<SshServerContext> createSessionChannel(Connection<SshServerContext> con)
			throws UnsupportedChannelException, PermissionDeniedException {
		return new UnsupportedSession(con, con.getContext());
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
	
	protected PublicKeySubsystem createPublicKeySubsystem(SessionChannel session) throws UnsupportedChannelException {
		
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
