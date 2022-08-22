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

package com.sshtools.server.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.server.components.SshKeyExchangeServerFactory;

public class DiffieHellmanGroupExchangeSha256JCE extends
		DiffieHellmanGroupExchangeSha1JCE {

	public static final String DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA256 = "diffie-hellman-group-exchange-sha256";

	public static class DiffieHellmanGroupExchangeSha256JCEFactory implements SshKeyExchangeServerFactory<DiffieHellmanGroupExchangeSha256JCE> {
		@Override
		public DiffieHellmanGroupExchangeSha256JCE create() throws NoSuchAlgorithmException, IOException {
			return new DiffieHellmanGroupExchangeSha256JCE();
		}

		@Override
		public String[] getKeys() {
			return new String[] { DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA256 };
		}
	}
	
	public DiffieHellmanGroupExchangeSha256JCE() {
		super("SHA-256", SecurityLevel.STRONG, 2000);
	}
	
	public String getAlgorithm() {
		return DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA256;
	}
}
