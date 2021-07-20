
package com.sshtools.common.util;

public class BlankLineEntry extends Entry<Void> {
	public BlankLineEntry() {
		super(null);
	}
	
	public String getFormattedEntry() {
		return "";
	}
}