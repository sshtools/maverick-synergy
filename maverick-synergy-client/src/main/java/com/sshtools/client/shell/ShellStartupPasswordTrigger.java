/* HEADER */
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
