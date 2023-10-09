package com.sshtools.common.publickey.authorized;

public class EnvironmentOption extends StringOption {

	EnvironmentOption(String value) {
		super("environment", value);
	}
	
	public EnvironmentOption(String key, String value) {
		super("environment", key + "=" + value);
	}

	public String getEnvironmentName() {
		return AuthorizedKeyFile.splitName(getValue());
	}
	
	public String getEnvironmentValue() {
		return AuthorizedKeyFile.splitValue(getValue());
	}

}
