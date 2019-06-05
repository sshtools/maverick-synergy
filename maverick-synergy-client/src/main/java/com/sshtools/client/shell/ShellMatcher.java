/* HEADER */
package com.sshtools.client.shell;

public interface ShellMatcher {

	enum Continue {
		MORE_CONTENT_NEEDED,
		CONTENT_MATCHES,
		CONTENT_DOES_NOT_MATCH
	}
	
    /**
     * Match a command output line against a defined pattern.
     * @param line the line of output to search
     * @param pattern the pattern required
     * @return boolean
     */
    public Continue matches(String line, String pattern);
}
