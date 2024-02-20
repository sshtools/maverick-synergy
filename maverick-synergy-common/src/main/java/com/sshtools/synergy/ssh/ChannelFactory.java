package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.Map;

import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.ssh.components.ComponentFactory;

public interface ChannelFactory<T extends SshContext> {

	ComponentFactory<ExecutableCommand> supportedCommands();
	
	ChannelNG<T> createChannel(String channeltype, SshConnection con) throws UnsupportedChannelException, PermissionDeniedException, ChannelOpenException;
    
	Subsystem createSubsystem(String name, SessionChannel session) throws UnsupportedChannelException, PermissionDeniedException;

	ExecutableCommand executeCommand(SessionChannel channel, String[] args, Map<String, String> environment) throws PermissionDeniedException, UnsupportedChannelException;

}
