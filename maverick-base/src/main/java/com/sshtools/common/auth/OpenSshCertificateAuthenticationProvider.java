package com.sshtools.common.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.OpenSshCertificate;

public class OpenSshCertificateAuthenticationProvider implements PublicKeyAuthenticationProvider {

	Set<SshPublicKey> caKeys = new HashSet<SshPublicKey>();
	
	public OpenSshCertificateAuthenticationProvider(SshPublicKey... caPublicKey) {
		caKeys.addAll(Arrays.asList(caPublicKey));
	}
	
	public OpenSshCertificateAuthenticationProvider(Collection<SshPublicKey> caPublicKeys) {
		caKeys.addAll(caPublicKeys);
	}
	
	public void addCAKey(SshPublicKey caKey) throws SshException {
		caKeys.add(caKey);
	}
	
	public void removeKey(SshPublicKey caKey) {
		caKeys.remove(caKey);
	}
	
	@Override
	public boolean isAuthorizedKey(SshPublicKey key, SshConnection con) throws IOException {
		
		if(!(key instanceof OpenSshCertificate)) {
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
	public Iterator<SshPublicKeyFile> getKeys(SshConnection con) throws PermissionDeniedException, IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove(SshPublicKey key, SshConnection con) throws IOException, PermissionDeniedException, SshException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(SshPublicKey key, String comment, SshConnection con)
			throws IOException, PermissionDeniedException, SshException {
		throw new UnsupportedOperationException();
	}

}
