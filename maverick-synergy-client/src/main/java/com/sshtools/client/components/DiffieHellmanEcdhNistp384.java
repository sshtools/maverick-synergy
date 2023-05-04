package com.sshtools.client.components;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.client.SshKeyExchangeClientFactory;
import com.sshtools.common.ssh.SecurityLevel;

public class DiffieHellmanEcdhNistp384 extends DiffieHellmanEcdh {
	
	private static final String KEY_EXCHANGE = "ecdh-sha2-nistp384";

	public static class DiffieHellmanEcdhNistp384Factory implements SshKeyExchangeClientFactory<DiffieHellmanEcdhNistp384> {
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
