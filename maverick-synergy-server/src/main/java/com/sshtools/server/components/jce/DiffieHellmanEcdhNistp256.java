
package com.sshtools.server.components.jce;

import com.sshtools.common.ssh.SecurityLevel;

public class DiffieHellmanEcdhNistp256 extends DiffieHellmanEcdh {

	public static final String DIFFIE_HELLMAN_ECDH_NISTP_256 = "ecdh-sha2-nistp256";
	
	public DiffieHellmanEcdhNistp256() {
		super("ecdh-sha2-nistp256", "secp256r1", "SHA-256", SecurityLevel.STRONG, 2256);
	}

}
