package com.sshtools.client;

import com.sshtools.common.ssh.Connection;

/**
 * Implements a keyboard-interactive callback that answers a single password prompt.
 */
public class PasswordOverKeyboardInteractiveCallback
			implements KeyboardInteractiveCallback {
		
		private String password;

		public PasswordOverKeyboardInteractiveCallback(
				PasswordAuthenticator pwdAuth) {
			password = pwdAuth.getPassword();
		}

		public void init(Connection<SshClientContext> con) {
		}
		
		/**
		 * Called by the <em>keyboard-interactive</em> authentication mechanism
		 * when the server requests information from the user. Each prompt
		 * should be displayed to the user with their response recorded within
		 * the prompt object.
		 * 
		 * @param name
		 * @param instruction
		 * @param prompts
		 * @param completor
		 */
		public void showPrompts(String name, String instruction,
				KeyboardInteractivePrompt[] prompts,
				KeyboardInteractivePromptCompletor completor) {
			for (int i = 0; i < prompts.length; i++) {
				prompts[i].setResponse(password);
			}
			completor.complete();
		}

	}