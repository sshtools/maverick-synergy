
package com.sshtools.common.publickey.authorized;

import java.util.Collection;

public class FromOption extends StringCollectionOption {

	FromOption(String values) {
		super("from", values);
	}

	public FromOption(Collection<String> values) {
		super("from", values);
	}

}
