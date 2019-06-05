package com.sshtools.bc.tests;

import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.tests.AbstractCipherTests;

public class BCCipherTest extends AbstractCipherTests {

	@Override
	protected void setUp() {
		JCEProvider.enableBouncyCastle(true);
	}

	@Override
	protected String getTestingJCE() {
		return "BC";
	}
}
