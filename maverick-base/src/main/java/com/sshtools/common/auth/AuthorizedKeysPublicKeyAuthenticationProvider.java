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
package com.sshtools.common.auth;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;

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

		try {
			AbstractFile file = getFile(con);

			InputStream in = file.getInputStream();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int read;
			byte[] buf = new byte[4096];

			try {
				while ((read = in.read(buf, 0, buf.length)) > 0) {
					out.write(buf, 0, read);
				}
			} catch (EOFException ex) {
			}

			in.close();

			/**
			 * Process the authorized keys
			 */
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new ByteArrayInputStream(out.toByteArray())));
			String line;
			SshPublicKeyFile pubfile;

			while ((line = reader.readLine()) != null) {
            	if(line.trim().equals("") || line.startsWith("#")) {
            		continue;
            	}
				/**
				 * Split up line and only reconstruct with key type as first token
				 * ignoring any other tokens before hand.
				 */
				String[] tokens = line.split(" ");
            	StringBuffer keyline = new StringBuffer();
            	boolean skip = true;
            	for(String t : tokens) {
            		if(skip) {
            			if(JCEComponentManager.getInstance().supportedPublicKeys().contains(t)) {
            				skip = false;
            			}
            		}
            		if(!skip) {
            			if(keyline.length() > 0) {
            				keyline.append(" ");
            			}
            			keyline.append(t);
            		}
            	}
                pubfile = SshPublicKeyFileFactory.parse(keyline.toString().getBytes("US-ASCII"));
                if (pubfile.toPublicKey().equals(key)) {
                    return true;
                }
			}

			return false;
		} catch (Throwable ex) {
			return false;
		}
	}

	public void add(SshPublicKey key, String comment,
			SshConnection con) throws IOException,
			PermissionDeniedException, SshException {

		AbstractFile file = getFile(con);

		SshPublicKeyFile keyFile = null;
		keyFile = SshPublicKeyFileFactory.create(key, comment,
				SshPublicKeyFileFactory.OPENSSH_FORMAT);

		OutputStream out = file.getOutputStream(file.exists());
		try {
			out.write((keyFile.toString() + "\n").getBytes("US-ASCII"));
		} finally {
			out.close();
		}
	}

	public void remove(SshPublicKey key, SshConnection con)
			throws IOException, PermissionDeniedException, SshException {

		AbstractFile file = getFile(con);
		SshPublicKeyFile pubfile;
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		PrintWriter outWriter = new PrintWriter(outBuffer);

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				file.getInputStream()));
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				pubfile = SshPublicKeyFileFactory.parse(line
						.getBytes("US-ASCII"));
				SshPublicKey spk = pubfile.toPublicKey();
				if (!spk.getFingerprint().equals(key.getFingerprint())) {
					outWriter.println(line);
				}
			}
		} finally {
			reader.close();
			outWriter.close();
		}
		OutputStream out = file.getOutputStream();
		try {
			out.write(outBuffer.toByteArray());
		} finally {
			out.close();
		}

	}

	public Iterator<SshPublicKeyFile> getKeys(SshConnection con)
			throws PermissionDeniedException, IOException {
		AbstractFile file = getFile(con);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				file.getInputStream()));
		SshPublicKeyFile pubfile;
		List<SshPublicKeyFile> keyFiles = new ArrayList<SshPublicKeyFile>();
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				pubfile = SshPublicKeyFileFactory.parse(line
						.getBytes("US-ASCII"));
				keyFiles.add(pubfile);
			}
		} finally {
			reader.close();
		}
		return keyFiles.iterator();
	}

	protected AbstractFile getFile(SshConnection connection)
			throws PermissionDeniedException, IOException {
		AbstractFileFactory<?> s = connection.getFileFactory();
		AbstractFile file = authorizedKeysFile.startsWith("/") ? s.getFile(
				authorizedKeysFile, connection) : s.getDefaultPath(connection)
				.resolveFile(authorizedKeysFile);
		return file;
	}

	@Override
	public boolean checkKey(SshPublicKey key, SshConnection con) throws IOException {
		return isAuthorizedKey(key, con);
	}
}
