/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.client.components;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.client.SshKeyExchangeClientFactory;
import com.sshtools.common.ssh.SecurityLevel;
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
	
	public static class DiffieHellmanGroupExchangeSha1JCEFactory implements SshKeyExchangeClientFactory<DiffieHellmanGroupExchangeSha1JCE> {
		@Override
		public DiffieHellmanGroupExchangeSha1JCE create() throws NoSuchAlgorithmException, IOException {
			return new DiffieHellmanGroupExchangeSha1JCE();
		}

		@Override
		public String[] getKeys() {
			return new String[] { DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA1 };
		}
	}
	
	/**
	 * Construct an uninitialized instance.
	 */
	public DiffieHellmanGroupExchangeSha1JCE() {
		super(DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA1, JCEAlgorithms.JCE_SHA1, SecurityLevel.WEAK, 1002);
	}

}
