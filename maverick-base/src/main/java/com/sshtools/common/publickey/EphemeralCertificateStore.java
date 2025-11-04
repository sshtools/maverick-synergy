package com.sshtools.common.publickey;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
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
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshCertificate;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.util.ExpiringConcurrentHashMap;

public class EphemeralCertificateStore {

	final Map<String,SshCertificate> userCertificates;
	final Duration ttl;
	final String keyAlgorithm;
	final SshKeyPair caKey;
	public EphemeralCertificateStore(Duration ttl, String keyAlgorithm, SshKeyPair caKey) {
		this.ttl = ttl;
		this.keyAlgorithm = keyAlgorithm;
		this.caKey = caKey;
		this.userCertificates = new ExpiringConcurrentHashMap<String, SshCertificate>(ttl.toMillis());
	}
	
	public SshKeyPair getKey(String username) throws SshException, IOException {

		SshCertificate key = userCertificates.get(username);
		if(Objects.isNull(key)) {
			SshKeyPair tmp = SshKeyPairGenerator.generateKeyPair(keyAlgorithm);
			key = SshCertificateAuthority.generateUserCertificate(tmp, 
					System.currentTimeMillis(),
					username,
					1,
					caKey);
			userCertificates.put(username, key);
		}
		
		return key;
	}
}
