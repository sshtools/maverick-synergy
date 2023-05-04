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
