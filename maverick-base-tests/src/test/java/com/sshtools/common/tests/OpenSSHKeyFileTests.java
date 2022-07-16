/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.tests;

import java.io.IOException;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.ssh.SshException;

public class OpenSSHKeyFileTests extends AbstractOpenSSHKeyFileTests {
	
	public void testEcdsaOriginalFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("openssh/old/ecdsa", "ecdsa_1", "ecdsa_1.pub", "ecdsa_1.fp", "ecdsa_1.fp.bb", "ecdsa_1_pw");
	}
	
	public void testEcdsaNewFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("openssh/old/ecdsa","ecdsa_1", "ecdsa_1.pub", "ecdsa_1.fp", "ecdsa_1.fp.bb", "ecdsa_n_pw");
	}
	
	public void testDsaOriginalFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("openssh/old/dsa","dsa_1", "dsa_1.pub", "dsa_1.fp", "dsa_1.fp.bb", "dsa_1_pw");
	}
	
	public void testDsaNewFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("openssh/old/dsa","dsa_1", "dsa_1.pub", "dsa_1.fp", "dsa_1.fp.bb", "dsa_n_pw");
	}
	
	public void testRsaOriginalFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("openssh/old/rsa","rsa_1", "rsa_1.pub", "rsa_1.fp", "rsa_1.fp.bb", "rsa_1_pw");
	}
	
	public void testRsaNewFormatKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("openssh/old/rsa","rsa_1", "rsa_1.pub", "rsa_1.fp", "rsa_1.fp.bb", "rsa_n_pw");
	}
}
