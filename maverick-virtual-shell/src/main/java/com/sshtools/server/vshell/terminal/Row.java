package com.sshtools.server.vshell.terminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.sshtools.server.vshell.terminal.Table.Alignment;

public class Row extends ArrayList<Cell<?>> {
	private static final long serialVersionUID = 1L;

	private Alignment defaultAlignment = Alignment.LEFT;
	private boolean strong;

	public Row(Cell<?>... cells) {
		this(Arrays.asList(cells));
	}

	public Row(int cells) {
		for (int i = 0; i < cells; i++) {
			add(new Cell<String>(""));
		}
	}

	public Row(Collection<Cell<?>> cells) {
		addAll(cells);
	}
	

	public boolean isStrong() {
		return strong;
	}

	public void setStrong(boolean strong) {
		this.strong = strong;
	}

	public Alignment getDefaultAlignment() {
		return defaultAlignment;
	}

	public void setDefaultAlignment(Alignment defaultAlignment) {
		this.defaultAlignment = defaultAlignment;
	}
}