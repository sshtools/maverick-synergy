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


package com.sshtools.client.shell;

import java.io.IOException;

public class ShellStartupPasswordTrigger implements ShellStartupTrigger {

	String passwordPromptExpression;
	String password;
	ShellMatcher matcher;
	
	
	public ShellStartupPasswordTrigger(String passwordPromptExpression, String password) {
		this(passwordPromptExpression, password, new ShellDefaultMatcher());
	}
	
	public ShellStartupPasswordTrigger(String passwordPromptExpression, String password, ShellMatcher matcher) {
		this.passwordPromptExpression = passwordPromptExpression;
		this.password = password;
		this.matcher = matcher;
	}
	
	public boolean canStartShell(String currentLine, ShellWriter writer) throws IOException {
		switch(matcher.matches(currentLine, passwordPromptExpression)) {
		case CONTENT_DOES_NOT_MATCH:
			throw new IOException("Expected password prompt but content does not match");
		case CONTENT_MATCHES:
			writer.typeAndReturn(password);
			return true;
		default:
			return false;
		}
	}

}
