package com.sshtools.server.vsession;

/*-
 * #%L
 * Virtual Sessions
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
