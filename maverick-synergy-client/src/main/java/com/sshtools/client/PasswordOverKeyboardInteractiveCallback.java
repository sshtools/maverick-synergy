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