package com.sshtools.common.publickey.authorized;

class NoArgOption extends Option<Void> {
	NoArgOption(String name) {
		super(name, null);
	}

	@Override
	public String getFormattedOption() {
		return getName();
	}
}