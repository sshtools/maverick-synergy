package com.sshtools.server.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.server.components.SshKeyExchangeServerFactory;

public class DiffieHellmanEcdhNistp384 extends DiffieHellmanEcdh {

	private static final String KEY_EXCHANGE = "ecdh-sha2-nistp384";

	public static class DiffieHellmanEcdhNistp384Factory implements SshKeyExchangeServerFactory<DiffieHellmanEcdhNistp384> {
		@Override
		public DiffieHellmanEcdhNistp384 create() throws NoSuchAlgorithmException, IOException {
			return new DiffieHellmanEcdhNistp384();
		}

		@Override
		public String[] getKeys() {
			return new String[] { KEY_EXCHANGE };
		}
	}
	
	public DiffieHellmanEcdhNistp384() {
		super(KEY_EXCHANGE, "secp384r1", "SHA-384", SecurityLevel.STRONG,2384);
	}

}
