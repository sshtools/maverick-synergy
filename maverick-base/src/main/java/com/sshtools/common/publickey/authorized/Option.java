package com.sshtools.common.publickey.authorized;

public abstract class Option<T> {
	
	String name;
	T value;
	Option(String name, T value) {
		this.name = name;
		this.value = value;
	}
	
	public abstract String getFormattedOption();

	public String getName() {
		return name;
	}
	
	public T getValue() {
		return value;
	}
}
