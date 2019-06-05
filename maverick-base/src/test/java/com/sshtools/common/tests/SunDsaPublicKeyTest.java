package com.sshtools.common.tests;

import com.sshtools.common.ssh.components.jce.JCEProvider;

public class SunDsaPublicKeyTest extends DsaPublicKeyTests {

	public void setUp() {
		JCEProvider.disableBouncyCastle();
	}

	@Override
	protected String getTestingJCE() {
		return "SUN";
	}
}
