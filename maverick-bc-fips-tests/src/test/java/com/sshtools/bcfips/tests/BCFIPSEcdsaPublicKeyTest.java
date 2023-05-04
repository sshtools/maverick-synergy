package com.sshtools.bcfips.tests;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.tests.EcdsaPublicKeyTests;

public class BCFIPSEcdsaPublicKeyTest extends EcdsaPublicKeyTests {

	@Override
	protected void setUp() {
		JCEProvider.enableBouncyCastle(true);
	}
	
	@Override
	protected String getTestingJCE() {
		return "BCFIPS";
	}
	
	@Override
	protected boolean isJCETested() {
		return true;
	}
}
