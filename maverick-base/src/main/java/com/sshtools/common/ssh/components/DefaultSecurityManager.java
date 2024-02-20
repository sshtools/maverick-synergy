package com.sshtools.common.ssh.components;

/*-
 * #%L
 * Base API
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.config.AdaptiveConfiguration;
import com.sshtools.common.ssh.SecurityLevel;

public class DefaultSecurityManager implements SecurityManager {

	AdaptiveConfiguration config;
	
	static Map<String,SecurityLevel> DEFAULTS = new HashMap<>();
	
	public DefaultSecurityManager() {
		this(Paths.get("security.cfg"));
	}
	
	public DefaultSecurityManager(Path path) {
		config = new AdaptiveConfiguration(path.toFile());
	}
	@Override
	public SecurityLevel getSecurityLevel(String algorithm) {
		
		return toSecurityLevel(config.getProperty(algorithm, "WEAK"));
	}
	private SecurityLevel toSecurityLevel(String val) {
		return SecurityLevel.valueOf(val);
	}

}
