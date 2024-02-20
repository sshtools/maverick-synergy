package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
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
import java.io.InputStream;
import java.util.List;

import com.sshtools.common.knownhosts.KnownHostsKeyVerification;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class KnownHostsTests extends AbstractKnownHostsTests {

	@Override
	public KnownHostsKeyVerification loadKnownHosts(InputStream in) throws SshException, IOException {
		return new KnownHostsKeyVerification(in);
	}

	class LegacyKnownHostsKeyVerification extends KnownHostsKeyVerification {

		public LegacyKnownHostsKeyVerification(InputStream knownHosts) throws SshException, IOException {
			super(knownHosts);
		}

		@Override
		public void onHostKeyMismatch(String host, List<SshPublicKey> allowedHostKey, SshPublicKey actualHostKey)
				throws SshException {
			
		}

		@Override
		public void onUnknownHost(String host, SshPublicKey key) throws SshException {
			
		}
		
	}
}
