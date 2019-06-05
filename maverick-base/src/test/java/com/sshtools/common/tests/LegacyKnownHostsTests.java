/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.sshtools.common.knownhosts.KnownHostsKeyVerification;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class LegacyKnownHostsTests extends AbstractKnownHostsTests {

	@Override
	public KnownHostsKeyVerification loadKnownHosts(InputStream in) throws SshException, IOException {
		return new LegacyKnownHostsKeyVerification(in);
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
