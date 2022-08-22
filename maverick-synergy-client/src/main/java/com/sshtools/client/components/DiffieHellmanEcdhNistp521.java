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

public class DiffieHellmanEcdhNistp521 extends DiffieHellmanEcdh {
	
	private static final String KEY_EXCHANGE = "ecdh-sha2-nistp521";

	public static class DiffieHellmanEcdhNistp521Factory implements SshKeyExchangeClientFactory<DiffieHellmanEcdhNistp521> {
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
