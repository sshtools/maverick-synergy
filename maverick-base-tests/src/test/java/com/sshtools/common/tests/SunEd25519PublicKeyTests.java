package com.sshtools.common.tests;

public class SunEd25519PublicKeyTests extends Ed25519PublicKeyTests {

	
	@Override
	protected String getTestingJCE() {
		return "SunEC";
	}
	
}
