
package com.sshtools.client.components;

import com.sshtools.common.ssh.SecurityLevel;

public class DiffieHellmanEcdhNistp384 extends DiffieHellmanEcdh {

	public DiffieHellmanEcdhNistp384() {
		super("ecdh-sha2-nistp384", "secp384r1", "SHA-384", SecurityLevel.STRONG,2384);
	}

}
