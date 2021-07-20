
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

	public Object getOrDefault(String name, Object defaultValue) {
		return containsKey(name) ? get(name) : defaultValue;
	}
}
