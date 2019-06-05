package com.sshtools.client.components;

import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

/**
 * Implementation of the required SSH Transport Protocol key exchange method
 * "diffie-hellman-group-exchange-sha1".
 */
public class DiffieHellmanGroupExchangeSha256JCE extends DiffieHellmanGroupExchange {


	/**
	 * Constant for the algorithm name "diffie-hellman-exchange-sha256".
	 */
	public static final String DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA256 = "diffie-hellman-group-exchange-sha256";

	/**
	 * Construct an uninitialized instance.
	 */
	public DiffieHellmanGroupExchangeSha256JCE() {
		super(DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA256, JCEAlgorithms.JCE_SHA256);
	}

}
