package com.sshtools.server.vsession;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ChannelNG;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.server.DefaultServerChannelFactory;
import com.sshtools.server.SshServerContext;

public class VirtualChannelFactory extends DefaultServerChannelFactory {

	CommandFactory<? extends ShellCommand>[] factories;
	
	@SafeVarargs
	public VirtualChannelFactory(CommandFactory<? extends ShellCommand>... factories) {
		this.factories = factories;
	}
	
	@Override
	protected ChannelNG<SshServerContext> createSessionChannel(SshConnection con)
			throws UnsupportedChannelException, PermissionDeniedException {
		return new VirtualShellNG(con,  new ShellCommandFactory(factories));
	}
}
