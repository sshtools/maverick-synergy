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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.files.memory;

import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public class InMemoryAbstractFileFactory implements AbstractFileFactory<InMemoryAbstractFile> {
	
	InMemoryFileSystem fs;
	
	public InMemoryAbstractFileFactory(InMemoryFileSystem fs) {
		this.fs = fs;
	} 

	/**
	 * Works on assumption, file will always have extension and directory will not
	 */
	@Override
	public InMemoryAbstractFile getFile(String path) throws PermissionDeniedException, IOException {
		if (path.startsWith("/")) {
			return new InMemoryAbstractFile(path, fs, this);
		} else if(path.equals("") || path.equals(".")){
			return new InMemoryAbstractFile("/", fs, this);
		} else {
			return new InMemoryAbstractFile("/" + path, fs, this);
		}
	}
	
	@Override
	public Event populateEvent(Event evt) {
		return evt;
	}

	@Override
	public InMemoryAbstractFile getDefaultPath() throws PermissionDeniedException, IOException {
		return getFile("");
	}
}
