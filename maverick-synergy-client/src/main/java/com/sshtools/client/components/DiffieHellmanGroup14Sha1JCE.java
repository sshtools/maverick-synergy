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
public class DiffieHellmanGroup14Sha1JCE extends DiffieHellmanGroup {

	/**
	 * Constant for the algorithm name "diffie-hellman-group14-sha1".
	 */
	public static final String DIFFIE_HELLMAN_GROUP14_SHA1 = "diffie-hellman-group14-sha1";

	public static class DiffieHellmanGroup14Sha1JCEFactory implements SshKeyExchangeClientFactory<DiffieHellmanGroup14Sha1JCE> {
		@Override
		public DiffieHellmanGroup14Sha1JCE create() throws NoSuchAlgorithmException, IOException {
			return new DiffieHellmanGroup14Sha1JCE();
		}

		@Override
		public String[] getKeys() {
			return new String[] { DIFFIE_HELLMAN_GROUP14_SHA1 };
		}
	}

	public DiffieHellmanGroup14Sha1JCE() {
		super(DIFFIE_HELLMAN_GROUP14_SHA1, JCEAlgorithms.JCE_SHA1, DiffieHellmanGroups.group14, SecurityLevel.WEAK,
				1001);
	}

}
