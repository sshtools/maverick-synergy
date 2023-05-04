package com.sshtools.client.components;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.client.SshKeyExchangeClientFactory;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

/**
 * Implementation of the required SSH Transport Protocol key exchange method
 * "diffie-hellman-group14-sha1".
 */
public class DiffieHellmanGroup18Sha512JCE extends DiffieHellmanGroup {

	/**
	 * Constant for the algorithm name "diffie-hellman-group16-sha512".
	 */
	public static final String DIFFIE_HELLMAN_GROUP18_SHA512 = "diffie-hellman-group18-sha512";

	public static class DiffieHellmanGroup18Sha512JCEFactory
			implements SshKeyExchangeClientFactory<DiffieHellmanGroup18Sha512JCE> {
		@Override
		public DiffieHellmanGroup18Sha512JCE create() throws NoSuchAlgorithmException, IOException {
			return new DiffieHellmanGroup18Sha512JCE();
		}

		@Override
		public String[] getKeys() {
			return new String[] { DIFFIE_HELLMAN_GROUP18_SHA512 };
		}
	}

	public DiffieHellmanGroup18Sha512JCE() {
		super(DIFFIE_HELLMAN_GROUP18_SHA512, JCEAlgorithms.JCE_SHA512, DiffieHellmanGroups.group18,
				SecurityLevel.PARANOID, 3018);
	}

}
