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
import java.util.Iterator;
import java.util.List;

import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.publickey.authorized.PublicKeyEntry;

public class KeysIterator implements Iterator<SshPublicKeyFile> {

	List<PublicKeyEntry> entries;
	
	public KeysIterator(List<PublicKeyEntry> entries) {
		this.entries = entries;
	}
	@Override
	public boolean hasNext() {
		return !entries.isEmpty();
	}

	@Override
	public SshPublicKeyFile next() {
		PublicKeyEntry e = entries.remove(0);
		try {
			return SshPublicKeyFileFactory.create(e.getValue(), e.getComment(), SshPublicKeyFileFactory.OPENSSH_FORMAT);
		} catch (IOException e1) {
			throw new IllegalStateException(e1.getMessage(), e1);
		}
	}

}
