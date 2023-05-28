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
package com.sshtools.common.files.direct;

import java.io.File;
import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.permissions.PermissionDeniedException;

/**
 * Deprecated, use {@link NioFile} and {@link NioFileFactory} instead.
 */
@Deprecated(since = "3.2.0", forRemoval = true)
public class DirectFileFactory extends AbstractDirectFileFactory<DirectFile> {

	File defaultPath = new File(".");
	boolean sandbox = false;
	
	public DirectFileFactory(File homeDirectory) {
		super(homeDirectory);
	}
	
	public DirectFileFactory(File homeDirectory, boolean sandbox) {
		super(homeDirectory);
		this.sandbox = sandbox;
	}
	
	public DirectFile getFile(String path)
			throws PermissionDeniedException, IOException {

		DirectFile file =  new DirectFile(path, this, homeDirectory);
		
		if(sandbox) {
			if(!file.getCanonicalPath().startsWith(homeDirectory.getCanonicalPath())) {
				throw new PermissionDeniedException("You cannot access paths other than your home directory");
			}
		}
		return file;
	}

	public Event populateEvent(Event evt) {
		return evt;
	}

}
