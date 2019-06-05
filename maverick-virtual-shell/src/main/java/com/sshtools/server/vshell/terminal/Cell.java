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