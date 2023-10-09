package com.sshtools.common.auth;

import java.io.IOException;
import java.util.Iterator;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * 
 * <p>Abstract implementation of a {@link PublicKeyAuthenticationProvider} 
 * that just provides {@link #getName()} implementation.
 * </p>
 * 
 * @author Lee David Painter
 */
public abstract class AbstractPublicKeyAuthenticationProvider implements
		PublicKeyAuthenticationProvider {

	@Override
	public boolean checkKey(SshPublicKey key, SshConnection con) throws IOException {
		return isAuthorizedKey(key, con);
	}

	@Override
	public Iterator<SshPublicKeyFile> getKeys(SshConnection con) throws PermissionDeniedException, IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove(SshPublicKey key, SshConnection con)
			throws IOException, PermissionDeniedException, SshException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(SshPublicKey key, String comment, SshConnection con)
			throws IOException, PermissionDeniedException, SshException {
		throw new UnsupportedOperationException();
	}

	public String getName() {
		return "publickey";
	}
}
