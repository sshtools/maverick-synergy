package com.sshtools.common.policy;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class SignaturePolicy {

	Set<String> supportedSignatures = new TreeSet<>();

	public SignaturePolicy() {
		
	}
	
	public SignaturePolicy(Collection<String> supportedSignatures) {
		this.supportedSignatures.addAll(supportedSignatures);
	}
	
	public Set<String> getSupportedSignatures() {
		return Collections.unmodifiableSet(supportedSignatures);
	}
}
