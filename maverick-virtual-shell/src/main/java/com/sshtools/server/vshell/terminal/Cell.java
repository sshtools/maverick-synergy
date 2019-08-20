/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.server.vshell.terminal;

import com.sshtools.server.vshell.terminal.Table.Alignment;

public class Cell<T extends Object> {
	private T value;
	private Alignment alignment;
	private boolean strong;
	private int maxWidth = -1;
	private int minWidth = 01;

	public Cell(T value) {
		this.value = value;
	}

	public boolean isStrong() {
		return strong;
	}

	public Cell<?> setStrong(boolean strong) {
		this.strong = strong;
		return this;
	}

	public Alignment getAlignment() {
		return alignment;
	}

	public Cell<?> setAlignment(Alignment alignment) {
		this.alignment = alignment;
		return this;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public String render() {
		return render(getValue());
	}

	protected String render(T value) {
		return value == null ? "<NULL>" : value.toString();
	}

	public Cell<?> setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
		return this;
	}
	
	public int getMaxWidth() {
		return maxWidth;
	}

	public Cell<?> setMinWidth(int minWidth) {
		this.minWidth = minWidth;
		return this;
	}
	
	public int getMinWidth() {
		return minWidth;
	}

}