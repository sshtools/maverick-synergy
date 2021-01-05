/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.ssh.components;

import com.sshtools.common.ssh.components.jce.OpenSshCertificate;

public class SshCertificate extends SshKeyPair {

	public static final int SSH_CERT_TYPE_USER = 1;
	public static final int SSH_CERT_TYPE_HOST = 2;
	
	OpenSshCertificate certificate;
	
	public SshCertificate(SshKeyPair pair, OpenSshCertificate certificate) {
		this.certificate = certificate;
		setPrivateKey(pair.getPrivateKey());
		setPublicKey(pair.getPublicKey());
	}
	
	public boolean isUserCertificate() {
		return certificate.isUserCertificate();
	}

	public boolean isHostCertificate() {
		return certificate.isHostCertificate();
	}

	public OpenSshCertificate getCertificate() {
		return certificate;
	}
}


