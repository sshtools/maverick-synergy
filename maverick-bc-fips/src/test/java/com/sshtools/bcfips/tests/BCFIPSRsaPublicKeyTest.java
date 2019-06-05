package com.sshtools.bcfips.tests;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.tests.RsaPublicKeyTests;

public class BCFIPSRsaPublicKeyTest extends RsaPublicKeyTests {

	@Override
	protected void setUp() {
		JCEProvider.enableBouncyCastle(true);
	}
	
	@Override
	protected String getTestingJCE() {
		return "BCFIPS";
	}
}
