
package com.sshtools.common.util;

public class CommentEntry extends Entry<String> {

	public CommentEntry(String value) {
		super(value);
	}
	
	public String getFormattedEntry() {
		return value;
	}
}