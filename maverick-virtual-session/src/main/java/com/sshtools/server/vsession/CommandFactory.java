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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
			T c = cls.getConstructor().newInstance();
			commands.put(c.getCommandName(), cls);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (SecurityException e) {
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
	
	public T createCommand(String command, SshConnection con) throws UnsupportedCommandException, IllegalAccessException, InstantiationException, IOException, PermissionDeniedException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return newInstance(command, con);
	}
	
	protected T newInstance(String command, SshConnection con) throws UnsupportedCommandException, IllegalAccessException,
			InstantiationException, IOException, PermissionDeniedException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if (!commands.containsKey(command)) {
			throw new UnsupportedCommandException(command + " is not a supported command");
		}

		Class<? extends T> cls = commands.get(command);
		T c = cls.getConstructor().newInstance();
		
		configureCommand(c, con);
		
		for (CommandConfigurator<T> configurator : configurators) {
			configurator.configure(c);
		}
		return c;
	}

	protected void configureCommand(T command, SshConnection con) throws IOException, PermissionDeniedException {
		
	}

}