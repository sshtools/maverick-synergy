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

public class Curve25519SHA256LibSshClient extends Curve25519SHA256Client {
	
	public static class Curve25519SHA256LibSshClientFactory implements SshKeyExchangeClientFactory<Curve25519SHA256LibSshClient> {
		@Override
		public Curve25519SHA256LibSshClient create() throws NoSuchAlgorithmException, IOException {
			return new Curve25519SHA256LibSshClient();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CURVE25519_SHA2_AT_LIBSSH_ORG };
		}
	}

	public static final String CURVE25519_SHA2_AT_LIBSSH_ORG = "curve25519-sha256@libssh.org";

	public Curve25519SHA256LibSshClient() {
		super(CURVE25519_SHA2_AT_LIBSSH_ORG);
	}
}
