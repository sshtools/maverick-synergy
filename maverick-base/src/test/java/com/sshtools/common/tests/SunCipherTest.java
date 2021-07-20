
package com.sshtools.common.tests;

import com.sshtools.common.ssh.components.jce.JCEProvider;

public class SunCipherTest extends AbstractCipherTests {

	public void setUp() {
		JCEProvider.disableBouncyCastle();
	}

	@Override
	protected String getTestingJCE() {
		return "SunJCE";
	}
}
