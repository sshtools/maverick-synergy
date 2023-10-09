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
