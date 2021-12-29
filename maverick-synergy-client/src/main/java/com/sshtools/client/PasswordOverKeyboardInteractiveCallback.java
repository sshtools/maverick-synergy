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
package com.sshtools.client;

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