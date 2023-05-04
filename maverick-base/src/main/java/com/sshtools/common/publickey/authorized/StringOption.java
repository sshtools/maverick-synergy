package com.sshtools.common.publickey.authorized;

abstract class StringOption extends Option<String> {

	StringOption(String name, String value) {
		super(name, value);
	}

	@Override
	public String getFormattedOption() {
		return getName() + "=\"" + getValue() + "\"";
	}
}
