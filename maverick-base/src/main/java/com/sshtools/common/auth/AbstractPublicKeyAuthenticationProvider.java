package com.sshtools.common.auth;

/*-
 * #%L
 * Base API
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
