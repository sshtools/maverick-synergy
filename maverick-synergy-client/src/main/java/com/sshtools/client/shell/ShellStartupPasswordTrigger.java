package com.sshtools.client.shell;

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
