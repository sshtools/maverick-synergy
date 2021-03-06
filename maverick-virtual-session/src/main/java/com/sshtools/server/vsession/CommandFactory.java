/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.server.vsession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public abstract class CommandFactory<T extends Command> {

	protected HashMap<String, Class<? extends T>> commands = new HashMap<String, Class<? extends T>>();
	protected List<CommandConfigurator<T>> configurators = new ArrayList<CommandConfigurator<T>>();
	
	public CommandFactory<T> addConfigurator(CommandConfigurator<T> configurator) {
		configurators.add(configurator);
		return this;
	}

	public CommandFactory<T> removeConfigurator(CommandConfigurator<T> configurator) {
		configurators.add(configurator);
		return this;
	}

	public CommandFactory<T> installCommand(String cmd, Class<? extends T> cls) {
		commands.put(cmd, cls);
		return this;
	}

	public CommandFactory<T> installCommand(Class<? extends T> cls) {
		try {
			T c = cls.newInstance();
			commands.put(c.getCommandName(), cls);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return this;
	}

	public CommandFactory<T> uninstallCommand(String cmd) {
		commands.remove(cmd);
		return this;
	}

	public java.util.Set<String> getSupportedCommands() {
		return commands.keySet();
	}

	public boolean supportsCommand(String command) {
		return commands.containsKey(command);
	}
	
	public T createCommand(String command, SshConnection con) throws UnsupportedCommandException, IllegalAccessException, InstantiationException, IOException, PermissionDeniedException {
		return newInstance(command, con);
	}
	
	protected T newInstance(String command, SshConnection con) throws UnsupportedCommandException, IllegalAccessException,
			InstantiationException, IOException, PermissionDeniedException {
		if (!commands.containsKey(command)) {
			throw new UnsupportedCommandException(command + " is not a supported command");
		}

		Class<? extends T> cls = commands.get(command);
		T c = cls.newInstance();
		
		configureCommand(c, con);
		
		for (CommandConfigurator<T> configurator : configurators) {
			configurator.configure(c);
		}
		return c;
	}

	protected void configureCommand(T command, SshConnection con) throws IOException, PermissionDeniedException {
		
	}

}