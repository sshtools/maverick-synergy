/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
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
