
package com.sshtools.client.components;

import com.sshtools.common.ssh.SecurityLevel;

public class DiffieHellmanEcdhNistp521 extends DiffieHellmanEcdh {

	public DiffieHellmanEcdhNistp521() {
		super("ecdh-sha2-nistp521", "secp521r1", "SHA-512", SecurityLevel.STRONG, 2521);
	}

}
