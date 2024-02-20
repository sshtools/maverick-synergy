package com.sshtools.server;

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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.sshtools.common.auth.AbstractPublicKeyAuthenticationProvider;
import com.sshtools.common.publickey.OpenSshCertificate;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class OpenSshCACertificateStoreImpl extends AbstractPublicKeyAuthenticationProvider {

	Set<SshPublicKey> caKeys = new HashSet<SshPublicKey>();
	
	public OpenSshCACertificateStoreImpl(SshPublicKey caPublicKey) {
		caKeys.add(caPublicKey);
	}
	
	public OpenSshCACertificateStoreImpl(Collection<SshPublicKey> caPublicKeys) {
		caKeys.addAll(caPublicKeys);
	}
	
	public void addCAKey(SshPublicKey caKey) throws SshException {
		caKeys.add(caKey);
	}
	
	public void removeKey(SshPublicKey caKey) {
		caKeys.remove(caKey);
	}
	
	public boolean isAuthorizedKey(SshPublicKey key, SshConnection con) {
		
		if(!(key.isCertificate())) {
			return false;
		}
		
		OpenSshCertificate cert = (OpenSshCertificate) key;
		
		if(cert.getType()!=OpenSshCertificate.SSH_CERT_TYPE_USER) {
			return false;
		}
		
		if(!new Date().after(cert.getValidAfter())) {
			return false;
		}
		
		if(!new Date().before(cert.getValidBefore())) {
			return false;
		}
		
		if(cert.getPrincipals().size() > 0) {
			if(!cert.getPrincipals().contains(con.getUsername())) {
				return false;
			}
		}
		
		for(SshPublicKey k : caKeys) {
			if(cert.getSignedBy().equals(k)) {
				return true;
			}
		}
		
		return false;
		
	}

	

	@Override
	public boolean checkKey(SshPublicKey key, SshConnection con) throws IOException {
		return isAuthorizedKey(key, con);
	}

}
