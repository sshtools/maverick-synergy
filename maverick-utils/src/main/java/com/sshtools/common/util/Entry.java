
package com.sshtools.common.util;

import java.io.IOException;

public abstract class Entry<T> {
	protected T value;
	protected Entry(T value) {
		this.value = value;
	}
	public T getValue() {
		return value;
	}
	public abstract String getFormattedEntry() throws IOException;
}