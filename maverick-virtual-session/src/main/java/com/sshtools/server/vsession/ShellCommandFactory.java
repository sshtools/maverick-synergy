
package com.sshtools.server.vsession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.commands.Alias;
import com.sshtools.server.vsession.commands.Catch;
import com.sshtools.server.vsession.commands.Clear;
import com.sshtools.server.vsession.commands.Date;
import com.sshtools.server.vsession.commands.Echo;
import com.sshtools.server.vsession.commands.Env;
import com.sshtools.server.vsession.commands.Exit;
import com.sshtools.server.vsession.commands.Help;
import com.sshtools.server.vsession.commands.Input;
import com.sshtools.server.vsession.commands.ShowLastError;
import com.sshtools.server.vsession.commands.Sleep;
import com.sshtools.server.vsession.commands.Source;
import com.sshtools.server.vsession.commands.Unalias;


public class ShellCommandFactory extends CommandFactory<ShellCommand> {

	final List<CommandFactory<? extends ShellCommand>> factories = new ArrayList<CommandFactory<? extends ShellCommand>>();

	
	@SafeVarargs
	public ShellCommandFactory(CommandFactory<? extends ShellCommand>... commandFactories) {
		installShellCommands();
		factories.addAll(Arrays.asList(commandFactories));
	}
	
	
	protected void installShellCommands() {
		
//		// Builtin 
		commands.put("alias", Alias.class);
		commands.put("unalias", Unalias.class);
//		
		commands.put("exit", Exit.class);
		commands.put("echo", Echo.class);
		commands.put("input", Input.class);
		commands.put("date", Date.class);

		commands.put("env", Env.class);
		commands.put("set", Env.class);
		
		commands.put("error", ShowLastError.class);
		commands.put("help", Help.class);
		
		commands.put("msh", Msh.class);
		commands.put("source", Source.class);
		
		commands.put("clear", Clear.class);
		commands.put("sleep", Sleep.class);
		commands.put("catch", Catch.class);
		
	}
	
	public void installFactory(CommandFactory<ShellCommand> factory) {
		factories.add(factory);
	}
	
	@Override
	public java.util.Set<String> getSupportedCommands() {
		Set<String> commands = new HashSet<String>();
		commands.addAll(super.getSupportedCommands());
		for(CommandFactory<? extends ShellCommand> factory : factories) {
			commands.addAll(factory.getSupportedCommands());
		}
		return commands;
	}
	
	@Override
	protected ShellCommand newInstance(String command, SshConnection con) throws UnsupportedCommandException,
			IllegalAccessException, InstantiationException, IOException, PermissionDeniedException {
		
		for(CommandFactory<? extends ShellCommand> factory : factories) {
			if(factory.supportsCommand(command)) {
				return factory.newInstance(command, con);
			}
		}
		
		return super.newInstance(command, con);
	}
	
	@Override
	public boolean supportsCommand(String command) {
		
		for(CommandFactory<? extends ShellCommand> factory : factories) {
			if(factory.supportsCommand(command)) {
				return true;
			}
		}
		
		return super.supportsCommand(command);
	}
	
	public RootShell createShell(SshConnection connection) throws PermissionDeniedException, IOException {

		RootShell shell = new RootShell(this, connection);
		configureCommand(shell, connection);
		return shell;
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}

}
