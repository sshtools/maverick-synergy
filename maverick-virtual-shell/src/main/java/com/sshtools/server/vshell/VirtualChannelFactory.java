package com.sshtools.server.vshell;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ChannelNG;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.server.DefaultServerChannelFactory;
import com.sshtools.server.SshServerContext;

public class VirtualChannelFactory extends DefaultServerChannelFactory {

	ShellCommandFactory commandFactory;
	
	public VirtualChannelFactory(ShellCommandFactory commandFactory) {
		this.commandFactory = commandFactory;
	}
	
	@Override
	protected ChannelNG<SshServerContext> createSessionChannel(Connection<SshServerContext> con)
			throws UnsupportedChannelException, PermissionDeniedException {
		return new VirtualShellNG(con, con.getContext(), commandFactory, new DefaultVirtualProcessFactory());
	}

	

}
