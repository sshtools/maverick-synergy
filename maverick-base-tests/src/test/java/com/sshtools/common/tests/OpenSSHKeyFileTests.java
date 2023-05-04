/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
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
