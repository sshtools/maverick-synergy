package com.sshtools.client;

import com.sshtools.common.ssh.Connection;

/**
 * Callback interface for keyboard-interactive authentication. Will be called when the server sends
 * authentication prompts.
 */
public interface KeyboardInteractiveCallback {
	
	public void init(Connection<SshClientContext> connection);
	
	public void showPrompts(String name, String instruction,
			KeyboardInteractivePrompt[] prompts, 
			KeyboardInteractivePromptCompletor completor);

	
}
