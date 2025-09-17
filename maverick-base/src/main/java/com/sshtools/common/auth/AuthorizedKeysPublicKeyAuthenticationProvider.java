package com.sshtools.common.auth;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.publickey.authorized.AuthorizedKeyFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * 
 * <p>
 * This class implements the OpenSSH style authorized_keys public key store.
 * </p>
 * 
 * @author Lee David Painter
 */
public class AuthorizedKeysPublicKeyAuthenticationProvider extends
		AbstractPublicKeyAuthenticationProvider {

	/**
	 * The path relative to the users home directory from which to load
	 * authorized keys
	 **/
	protected String authorizedKeysFile = ".ssh/authorized_keys";

	/**
	 * Create a default <em>authorized_keys</em> store that reads keys from
	 * <em>.ssh/authorized_keys</em>.
	 */
	public AuthorizedKeysPublicKeyAuthenticationProvider() {
	}

	/**
	 * Create an authorized keys stores that reads keys from a custom location.
	 * 
	 * @param authorizedKeysFile
	 *            String
	 */
	public AuthorizedKeysPublicKeyAuthenticationProvider(
			String authorizedKeysFile) {
		this.authorizedKeysFile = authorizedKeysFile;
	}

	/**
	 * Checks the given public key by comparing it against the public keys
	 * stored in the users <em>authorized_keys</em> file.
	 * 
	 * @param key
	 *            SshPublicKey
	 * @param sessionid
	 *            byte[]
	 * @param authenticationProvider
	 *            AuthenticationProvider
	 * @return boolean
	 */
	public boolean isAuthorizedKey(SshPublicKey key,
			SshConnection con) {

		try(InputStream in = getAuthorizedKeysInputStream(con)) {
			AuthorizedKeyFile file = new AuthorizedKeyFile();
			file.load(in);
			return file.isAuthorizedKey(key);
		} catch (IOException | PermissionDeniedException e) {
			return false;
		} 
	}

	protected InputStream getAuthorizedKeysInputStream(SshConnection con) throws PermissionDeniedException, IOException {
		AbstractFile file = getFile(con);
		return file.getInputStream();
	}
	
	protected OutputStream getAuthorizedKeysOutputStream(SshConnection con) throws PermissionDeniedException, IOException {
		AbstractFile file = getFile(con);
		return file.getOutputStream();
	}

	public void add(SshPublicKey key, String comment,
			SshConnection con) throws IOException,
			PermissionDeniedException, SshException {

		try(InputStream in = getAuthorizedKeysInputStream(con)) {
			AuthorizedKeyFile file = new AuthorizedKeyFile();
			file.load(in);
			if(!file.isAuthorizedKey(key)) {
				file.addKey(key, comment);
				try(OutputStream out = getAuthorizedKeysOutputStream(con)) {
					file.save(out);
				}
			}
		} 
	}

	public void remove(SshPublicKey key, SshConnection con)
			throws IOException, PermissionDeniedException, SshException {

		try(InputStream in = getAuthorizedKeysInputStream(con)) {
			AuthorizedKeyFile file = new AuthorizedKeyFile();
			file.load(in);
			if(file.isAuthorizedKey(key)) {
				file.removeKeys(key);
				try(OutputStream out = getAuthorizedKeysOutputStream(con)) {
					file.save(out);
				}
			}
		} 
	}

	public Iterator<SshPublicKeyFile> getKeys(SshConnection con)
			throws PermissionDeniedException, IOException {
		try(InputStream in = getAuthorizedKeysInputStream(con)) {
			AuthorizedKeyFile file = new AuthorizedKeyFile();
			file.load(in);
			return new KeysIterator(new ArrayList<>(file.getKeys()));
		}
	}

	protected AbstractFile getFile(SshConnection con)
			throws PermissionDeniedException, IOException {
		AbstractFileFactory<?> s = con.getContext()
				.getPolicy(FileSystemPolicy.class)
					.getFileFactory().getFileFactory(con);
		AbstractFile file = authorizedKeysFile.startsWith("/") ? s.getFile(
				authorizedKeysFile) : s.getDefaultPath()
				.resolveFile(authorizedKeysFile);
		return file;
	}

	@Override
	public boolean checkKey(SshPublicKey key, SshConnection con) throws IOException {
		return isAuthorizedKey(key, con);
	}
}
