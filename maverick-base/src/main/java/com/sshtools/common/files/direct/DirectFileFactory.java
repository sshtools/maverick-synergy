package com.sshtools.common.files.direct;

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

import java.io.File;
import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.permissions.PermissionDeniedException;

/**
 * Deprecated, use {@link NioFile} and {@link NioFileFactory} instead.
 */
@Deprecated(since = "3.1.0", forRemoval = true)
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
