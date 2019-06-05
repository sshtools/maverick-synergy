package com.sshtools.server.components.jce;

public class DiffieHellmanEcdhNistp521 extends DiffieHellmanEcdh {

	public static final String DIFFIE_HELLMAN_ECDH_NISTP_521 = "ecdh-sha2-nistp521";
	
	public DiffieHellmanEcdhNistp521() {
		super("ecdh-sha2-nistp521", "secp521r1", "SHA-512");
	}

}
