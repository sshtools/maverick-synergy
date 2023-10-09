package com.sshtools.bcfips.tests;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.tests.AbstractCipherTests;

public class BCFIPSCipherTest extends AbstractCipherTests {

	@Override
	protected void setUp() {
		JCEProvider.enableBouncyCastle(true);
	}

	@Override
	protected String getTestingJCE() {
		return "BCFIPS";
	}

	@Override
	public void test3DESCBC() throws NoSuchAlgorithmException, IOException {
		// Not processed due to limit on number of bytes 3DES can process
	}

	@Override
	public void test3DESCTR() throws NoSuchAlgorithmException, IOException {
		// Not processed due to limit on number of bytes 3DES can process
	}
	
	
}
