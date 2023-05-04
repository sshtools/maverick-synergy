package com.sshtools.common.publickey.authorized;

import java.util.Collection;

public class PrincipalsOption extends StringCollectionOption {

	PrincipalsOption(String values) {
		super("principals", values);
	}

	public PrincipalsOption(Collection<String> values) {
		super("principals", values);
	}

}
