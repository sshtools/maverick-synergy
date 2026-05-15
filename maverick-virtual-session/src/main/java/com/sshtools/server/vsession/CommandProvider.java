package com.sshtools.server.vsession;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public interface CommandProvider<T extends Command> {

	/**
	 * Get the names of the supported commands.
	 * 
	 * @return java.util.Set<String>
	 */
	public java.util.Set<String> getSupportedCommands();

	/**
	 * Create a new instance of the command with the given name.
	 * 
	 * @param command
	 *            String
	 * @return T
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 * @throws InstantiationException 
	 * @throws IllegalAccessException 
	 * @throws UnsupportedCommandException 
	 */
	public T createCommand(String command, SshConnection con) throws UnsupportedCommandException;

	
	public boolean supportsCommand(String command);


}
