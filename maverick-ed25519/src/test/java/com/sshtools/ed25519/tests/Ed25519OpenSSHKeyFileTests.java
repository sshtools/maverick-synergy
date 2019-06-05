package com.sshtools.ed25519.tests;

import java.io.IOException;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.tests.AbstractOpenSSHKeyFileTests;

public class Ed25519OpenSSHKeyFileTests extends AbstractOpenSSHKeyFileTests {

	public void testEd25519Keys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("ed25519_1", "ed25519_1.pub", "ed25519_1.fp", "ed25519_1.fp.bb", "ed25519_1_pw");
	}
}
