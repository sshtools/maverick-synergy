
package com.sshtools.server.components.jce;

import com.sshtools.common.ssh.SecurityLevel;

public class DiffieHellmanEcdhNistp384 extends DiffieHellmanEcdh {

	public static final String DIFFIE_HELLMAN_ECDH_NISTP_384 = "ecdh-sha2-nistp384";
	
	public DiffieHellmanEcdhNistp384() {
		super("ecdh-sha2-nistp384", "secp384r1", "SHA-384", SecurityLevel.STRONG,2384);
	}

}
