package com.sshtools.bc.tests;

import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.tests.DsaPublicKeyTests;

public class BCDsaPublicKeyTest extends DsaPublicKeyTests {
	
	@Override
	protected void setUp() {
		JCEProvider.enableBouncyCastle(true);
	}

	@Override
	protected String getTestingJCE() {
		return "BC";
	}
}
