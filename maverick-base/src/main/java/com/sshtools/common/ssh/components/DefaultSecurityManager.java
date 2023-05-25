/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.ssh.components;

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
