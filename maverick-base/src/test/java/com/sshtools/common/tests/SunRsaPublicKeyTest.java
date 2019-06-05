package com.sshtools.common.tests;

import com.sshtools.common.ssh.components.jce.JCEProvider;

public class SunRsaPublicKeyTest extends RsaPublicKeyTests {

	public void setUp() {
		JCEProvider.disableBouncyCastle();
	}

	@Override
	protected String getTestingJCE() {
		return "SunRsaSign";
	}

}
