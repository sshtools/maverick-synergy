/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.publickey.putty;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyProvider;

public class PuTTYPrivateKeyProvider implements SshPrivateKeyProvider {

	@Override
	public boolean isFormatted(byte[] formattedkey) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(formattedkey)));
		try {
			String line = reader.readLine();

			return (line != null && (line.startsWith("PuTTY-User-Key-File-3:")
					|| line.startsWith("PuTTY-User-Key-File-2:") || line.equals("PuTTY-User-Key-File-1:")));
		} catch (IOException ex) {
			return false;
		}
	}

	@Override
	public SshPrivateKeyFile create(byte[] formattedkey) throws IOException {
		if (!isFormatted(formattedkey)) {
			throw new IOException("Key is not formatted in the PuTTY key format!");
		}
		return new PuTTYPrivateKeyFile(formattedkey);
	}

}
