

package com.sshtools.client.shell;

public class ShellDefaultMatcher implements ShellMatcher {

	public Continue matches(String line, String pattern) {
		
		boolean match = false;
		for(int i=0;i<line.length();i++) {
			match = line.charAt(i) == pattern.charAt(i);
		}
		
		if(match && pattern.length() == line.length()) {
			return Continue.CONTENT_MATCHES;
		} else if(match) {
			return Continue.MORE_CONTENT_NEEDED;
		} else {
			return Continue.CONTENT_DOES_NOT_MATCH;
		}
	}

}
