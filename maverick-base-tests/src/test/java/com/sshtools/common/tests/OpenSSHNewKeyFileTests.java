package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.jce.JCEProvider;

public class OpenSSHNewKeyFileTests extends AbstractOpenSSHKeyFileTests {

	public void testEd25519Keys() throws IOException, InvalidPassphraseException, SshException {
		
		JCEProvider.enableBouncyCastle(false);
		ComponentManager.reset();
		
		performPrivateKeyTests("openssh/new/ed25519", "ed25519-nopass.key", "ed25519.key.pub", 
				"ed25519.fp", "ed25519.bb", "ed25519.key");
		
		JCEProvider.disableBouncyCastle();
		ComponentManager.reset();
	}
	
	public void testEd448Keys() throws IOException, InvalidPassphraseException, SshException {
		
		JCEProvider.enableBouncyCastle(false);
		ComponentManager.reset();
		
		performPrivateKeyTests("openssh/new/ed448", "ed448-nopass.key", "ed448.pub", 
				"ed448.fp", "ed448.bb", "ed448-withpass.key");
		
		JCEProvider.disableBouncyCastle();
		ComponentManager.reset();
	}
	
	public void testEcdsa() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("openssh/new/ecdsa/256", "ecdsa-nopass.key", "ecdsa.pub", 
				"ecdsa.fp", "ecdsa.bb", "ecdsa-withpass.key");
	}

	public void testRsa2048BitsKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("openssh/new/rsa/2048", "rsa-nopass.key", "rsa.key.pub", 
				"rsa.fp", "rsa.bb", "rsa.key");
	}

}
