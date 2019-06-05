package com.sshtools.server.components.jce;

public class DiffieHellmanGroupExchangeSha256JCE extends
		DiffieHellmanGroupExchangeSha1JCE {

	public static final String DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA256 = "diffie-hellman-group-exchange-sha256";
	public DiffieHellmanGroupExchangeSha256JCE() {
		super("SHA-256");
	}
	
	public String getAlgorithm() {
		return DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA256;
	}
}
