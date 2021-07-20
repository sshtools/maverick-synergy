
package com.sshtools.common.publickey.authorized;

public class TunnelOption extends StringOption {

	public TunnelOption(String value) {
		super("tunnel", value);
	}

	@Override
	public String getFormattedOption() {
		return getName() + "=\"" + getValue() + "\"";
	}

}
