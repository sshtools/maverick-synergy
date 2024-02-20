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
