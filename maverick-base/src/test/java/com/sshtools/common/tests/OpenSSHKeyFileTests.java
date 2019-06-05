package com.sshtools.common.tests;

import java.io.IOException;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.ssh.SshException;

public class OpenSSHKeyFileTests extends AbstractOpenSSHKeyFileTests {
	
	public void testEcdsaOriginalFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("ecdsa_1", "ecdsa_1.pub", "ecdsa_1.fp", "ecdsa_1.fp.bb", "ecdsa_1_pw");
	}
	
	public void testEcdsaNewFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("ecdsa_1", "ecdsa_1.pub", "ecdsa_1.fp", "ecdsa_1.fp.bb", "ecdsa_n_pw");
	}
	
	public void testDsaOriginalFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("dsa_1", "dsa_1.pub", "dsa_1.fp", "dsa_1.fp.bb", "dsa_1_pw");
	}
	
	public void testDsaNewFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("dsa_1", "dsa_1.pub", "dsa_1.fp", "dsa_1.fp.bb", "dsa_n_pw");
	}
	
	public void testRsaOriginalFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("rsa_1", "rsa_1.pub", "rsa_1.fp", "rsa_1.fp.bb", "rsa_1_pw");
	}
	
	public void testRsaNewFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("rsa_1", "rsa_1.pub", "rsa_1.fp", "rsa_1.fp.bb", "rsa_n_pw");
	}
}
