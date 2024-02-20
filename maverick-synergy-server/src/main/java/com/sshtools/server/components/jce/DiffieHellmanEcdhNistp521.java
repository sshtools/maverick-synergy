package com.sshtools.server.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.server.components.SshKeyExchangeServerFactory;

public class DiffieHellmanEcdhNistp521 extends DiffieHellmanEcdh {

	private static final String KEY_EXCHANGE = "ecdh-sha2-nistp521";

	public static class DiffieHellmanEcdhNistp521Factory implements SshKeyExchangeServerFactory<DiffieHellmanEcdhNistp521> {
		@Override
		public DiffieHellmanEcdhNistp521 create() throws NoSuchAlgorithmException, IOException {
			return new DiffieHellmanEcdhNistp521();
		}

		@Override
		public String[] getKeys() {
			return new String[] { KEY_EXCHANGE };
		}
	}
	
	public DiffieHellmanEcdhNistp521() {
		super(KEY_EXCHANGE, "secp521r1", "SHA-512", SecurityLevel.STRONG, 2521);
	}

}
