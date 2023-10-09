package com.sshtools.server.vsession;

import java.util.HashMap;

public class Environment extends HashMap<String, Object> {

	private static final long serialVersionUID = 1L;
	public final static String ENV_HOME = "HOME";

	public Environment(Environment environment) {
		super(environment);
	}

	public Environment() {
		super();
	}

	@SuppressWarnings("unchecked")
	public <T> T getOrDefault(String name, T defaultValue) {
		return containsKey(name) ? (T) get(name) : defaultValue;
	}
}
