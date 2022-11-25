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
