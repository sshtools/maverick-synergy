package com.sshtools.bcfips.tests;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.tests.DsaPublicKeyTests;

public class BCFIPSDsaPublicKeyTest extends DsaPublicKeyTests {

	@Override
	protected void setUp() {
		JCEProvider.enableBouncyCastle(true);
	}
	
	@Override
	protected String getTestingJCE() {
		return "BCFIPS";
	}
}
