package com.sshtools.client.components;

public class DiffieHellmanEcdhNistp256 extends DiffieHellmanEcdh {

	public DiffieHellmanEcdhNistp256() {
		super("ecdh-sha2-nistp256", "secp256r1", "SHA-256");
	}

}
