package com.sshtools.common.sshd.config;

import java.util.Stack;


/**
 * Cursor maintains track of current entry.
 * Note: This is not per line entry it is per entry viz Global or Match
 */
public class SshdConfigFileCursor {
	private Stack<Entry> currentEntryStack = new Stack<>();
	
	public void set(Entry currentEntry) {
		this.currentEntryStack.push(currentEntry);
	}
	
	public Entry get() {
		return this.currentEntryStack.peek();
	}
	
	public Entry remove() {
		if (!this.currentEntryStack.isEmpty()) {
			return this.currentEntryStack.pop();
		}
		return null;
	}
}
