package com.sshtools.common.ssh;

import java.util.Map;

import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.permissions.PermissionDeniedException;

public interface ChannelFactory<T extends SshContext> {

	ChannelNG<T> createChannel(String channeltype, Connection<T> con) throws UnsupportedChannelException, PermissionDeniedException;
    
	Subsystem createSubsystem(String name, SessionChannel session) throws UnsupportedChannelException, PermissionDeniedException;

	ExecutableCommand executeCommand(String[] args, Map<String, String> environment) throws PermissionDeniedException, UnsupportedChannelException;


}
