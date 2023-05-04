/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
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
