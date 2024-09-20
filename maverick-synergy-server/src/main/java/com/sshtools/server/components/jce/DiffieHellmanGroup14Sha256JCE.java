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

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.server.components.SshKeyExchangeServerFactory;

public class DiffieHellmanGroup14Sha256JCE extends DiffieHellmanGroup {

	public static final String DIFFIE_HELLMAN_GROUP14_SHA256 = "diffie-hellman-group14-sha256";

	public static class DiffieHellmanGroup14Sha256JCEFactory implements SshKeyExchangeServerFactory<DiffieHellmanGroup14Sha256JCE> {
		@Override
		public DiffieHellmanGroup14Sha256JCE create() throws NoSuchAlgorithmException, IOException {
			return new DiffieHellmanGroup14Sha256JCE();
		}

		@Override
		public String[] getKeys() {
			return new String[] { DIFFIE_HELLMAN_GROUP14_SHA256 };
		}
	}
	
	public DiffieHellmanGroup14Sha256JCE() {
		super(DIFFIE_HELLMAN_GROUP14_SHA256, JCEAlgorithms.JCE_SHA256, DiffieHellmanGroups.group14, SecurityLevel.STRONG, 2001);
	}

}
