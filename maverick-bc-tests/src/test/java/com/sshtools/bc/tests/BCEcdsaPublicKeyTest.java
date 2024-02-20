package com.sshtools.bc.tests;

import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.tests.EcdsaPublicKeyTests;

public class BCEcdsaPublicKeyTest extends EcdsaPublicKeyTests {

	@Override
	protected void setUp() {
		JCEProvider.enableBouncyCastle(true);
	}

	@Override
	protected String getTestingJCE() {
		return "BC";
	}

	@Override
	protected boolean isJCETested() {
		return true;
	}

}