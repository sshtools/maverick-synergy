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

/**
 *  Represents a keyboard-interactive prompt.
 */
public class KeyboardInteractivePrompt {
	private String prompt;
	private String response;
	private boolean echo;

	/**
	 * Creates a new KBIPrompt object.
	 *
	 * @param prompt
	 * @param echo
	 */
	public KeyboardInteractivePrompt(String prompt, boolean echo) {
		this.prompt = prompt;
		this.echo = echo;
	}

	/**
	 * Get the prompt message to display to the user
	 *
	 * @return String
	 */
	public String getPrompt() {
		return prompt;
	}

	/**
	 * <tt>true</tt> if the user response should be echo'd to the display,
	 * otherwise <tt>false</tt>.
	 *
	 * @return boolean
	 */
	public boolean echo() {
		return echo;
	}

	/**
	 * Set the user's response for this prompt.
	 *
	 * @param response
	 */
	public void setResponse(String response) {
		this.response = response;
	}

	/**
	 * Get the user's response for this prompt.
	 *
	 * @return String
	 */
	public String getResponse() {
		return response;
	}
}
