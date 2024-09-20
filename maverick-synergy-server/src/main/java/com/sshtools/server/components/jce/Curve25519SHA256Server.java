package com.sshtools.server.components.jce;

/*-
 * #%L
 * Server API
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

import com.sshtools.server.components.SshKeyExchangeServerFactory;

public class Curve25519SHA256Server extends Curve25519SHA256LibSshServer {

	public static final String CURVE25519_SHA2 = "curve25519-sha256";

	public static class Curve25519SHA256ServerFactory implements SshKeyExchangeServerFactory<Curve25519SHA256Server> {
		@Override
		public Curve25519SHA256Server create() throws NoSuchAlgorithmException, IOException {
			return new Curve25519SHA256Server();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CURVE25519_SHA2 };
		}
	}
	
	public Curve25519SHA256Server() {
		super(CURVE25519_SHA2);
	}	
}
