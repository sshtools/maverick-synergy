package com.sshtools.client.components;

/*-
 * #%L
 * Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
