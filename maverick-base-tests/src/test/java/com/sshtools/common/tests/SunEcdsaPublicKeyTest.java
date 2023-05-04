package com.sshtools.common.tests;

import com.sshtools.common.ssh.components.jce.JCEProvider;

public class SunEcdsaPublicKeyTest extends EcdsaPublicKeyTests {

	public void setUp() {
		JCEProvider.setECDSAAlgorithmName("EC");
		JCEProvider.disableBouncyCastle();
	}

	@Override
	protected String getTestingJCE() {
		return "SunEC";
	}
	
	@Override
	protected boolean isJCETested() {
		return true;
	}
}
