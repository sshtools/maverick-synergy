package com.sshtools.client.components;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.client.SshKeyExchangeClientFactory;
import com.sshtools.common.ssh.SecurityLevel;

public class DiffieHellmanEcdhNistp256 extends DiffieHellmanEcdh {
	
	private static final String KEY_EXCHANGE = "ecdh-sha2-nistp256";

	public static class DiffieHellmanEcdhNistp256Factory implements SshKeyExchangeClientFactory<DiffieHellmanEcdhNistp256> {
		@Override
		public DiffieHellmanEcdhNistp256 create() throws NoSuchAlgorithmException, IOException {
			return new DiffieHellmanEcdhNistp256();
		}

		@Override
		public String[] getKeys() {
			return new String[] { KEY_EXCHANGE };
		}
	}

	public DiffieHellmanEcdhNistp256() {
		super(KEY_EXCHANGE, "secp256r1", "SHA-256", SecurityLevel.STRONG, 2256);
	}

}
