package com.sshtools.server.vshell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.commands.Alias;
import com.sshtools.server.vshell.commands.Catch;
import com.sshtools.server.vshell.commands.Clear;
import com.sshtools.server.vshell.commands.Date;
import com.sshtools.server.vshell.commands.Echo;
import com.sshtools.server.vshell.commands.Env;
import com.sshtools.server.vshell.commands.Exit;
import com.sshtools.server.vshell.commands.Help;
import com.sshtools.server.vshell.commands.Kill;
import com.sshtools.server.vshell.commands.Mem;
import com.sshtools.server.vshell.commands.Ps;
import com.sshtools.server.vshell.commands.ShowLastError;
import com.sshtools.server.vshell.commands.Sleep;
import com.sshtools.server.vshell.commands.Term;
import com.sshtools.server.vshell.commands.Unalias;
import com.sshtools.server.vshell.commands.Who;


public class ShellCommandFactory extends CommandFactory<ShellCommand> {

	final List<CommandFactory<? extends ShellCommand>> factories = new ArrayList<CommandFactory<? extends ShellCommand>>();
	String welcomeText;
	
	@SafeVarargs
	public ShellCommandFactory(String welcomeText, CommandFactory<? extends ShellCommand>... commandFactories) {
		this.welcomeText = welcomeText;
		installShellCommands();
		factories.addAll(Arrays.asList(commandFactories));
	}
	
	
	protected void installShellCommands() {
		
		// Builtin 
		commands.put("alias", Alias.class);
		commands.put("unalias", Unalias.class);
		
		// The basic shell operations
		commands.put("echo", Echo.class);
		commands.put("date", Date.class);
		
		commands.put("env", Env.class);
		commands.put("exit", Exit.class);
		commands.put("set", Env.class);
		commands.put("export", Env.class);
		commands.put("error", ShowLastError.class);
		commands.put("help", Help.class);
		commands.put("kill", Kill.class);
		commands.put("who", Who.class);
		commands.put("mem", Mem.class);
		commands.put("term", Term.class);
		commands.put("msh", Msh.class);
		commands.put("ps", Ps.class);
		commands.put("clear", Clear.class);
		commands.put("sleep", Sleep.class);
		commands.put("catch", Catch.class);
		
	}
	
	protected void installFactory(ShellCommandFactory factory) {
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

		RootShell shell = new RootShell(this, connection, welcomeText);
		configureCommand(shell, connection);
		return shell;
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}

}
