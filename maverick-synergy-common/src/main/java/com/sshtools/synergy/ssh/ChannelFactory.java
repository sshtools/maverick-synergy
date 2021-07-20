
package com.sshtools.synergy.ssh;

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
