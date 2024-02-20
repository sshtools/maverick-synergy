package com.sshtools.client;

/*-
 * #%L
 * Client API
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

import com.sshtools.common.ssh.SshConnection;

/**
 * Implements a keyboard-interactive callback that answers a single password prompt.
 */
public class PasswordOverKeyboardInteractiveCallback
			implements KeyboardInteractiveCallback {
		
		
		PasswordAuthenticator auth;
		public PasswordOverKeyboardInteractiveCallback(
				PasswordAuthenticator auth) {
			this.auth = auth;
		}

		public void init(SshConnection con) {
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
				prompts[i].setResponse(auth.getPassword());
			}
			completor.complete();
		}

	}
