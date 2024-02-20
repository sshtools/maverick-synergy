package com.sshtools.bc.tests;

import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.tests.Ed25519PublicKeyTests;

public class BCEd25519PublicKeyTests extends Ed25519PublicKeyTests {

	public void setUp() {
		JCEProvider.enableBouncyCastle(true);
		ComponentManager.reset();
	}

	public void tearDown() {
		JCEProvider.disableBouncyCastle();
		ComponentManager.reset();
	}
	
	@Override
	protected String getTestingJCE() {
		return "BC";
	}

}
