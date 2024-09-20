package com.sshtools.common.publickey.putty;

/*-
 * #%L
 * PuTTY Key Support
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
