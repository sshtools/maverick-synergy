package com.sshtools.client.components;

import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

/**
 * Implementation of the required SSH Transport Protocol key exchange method
 * "diffie-hellman-group-exchange-sha1".
 */
public class DiffieHellmanGroupExchangeSha1JCE extends DiffieHellmanGroupExchange {


	/**
	 * Constant for the algorithm name "diffie-hellman-group1-sha1".
	 */
	public static final String DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA1 = "diffie-hellman-group-exchange-sha1";

	/**
	 * Construct an uninitialized instance.
	 */
	public DiffieHellmanGroupExchangeSha1JCE() {
		super(DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA1, JCEAlgorithms.JCE_SHA1);
	}

}
