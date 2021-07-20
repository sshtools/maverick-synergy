
package com.sshtools.common.publickey.authorized;

import java.util.Collection;

public class PermitOpenOption extends StringCollectionOption {

	PermitOpenOption(String values) {
		super("permitopen", values);
	}
	
	public PermitOpenOption(Collection<String> values) {
		super("permitopen", values);
	}

	

}
