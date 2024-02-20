package com.sshtools.common.files.vfs;

/*-
 * #%L
 * Virtual File System
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
import java.nio.file.Path;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.direct.NioFile;
import com.sshtools.common.files.direct.NioFileFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;

/**
 * Deprecated. Use {@link NioFileFactory} and {@link NioFile}.
 */
@Deprecated(since = "3.1.0", forRemoval = true)
public class PathFileFactory implements AbstractFileFactory<PathFile> {
	
	private Path base;

	public PathFileFactory(Path base) {
		this.base = base;
	}

	@Override
	public PathFile getFile(String path) throws PermissionDeniedException, IOException {
		if(Log.isTraceEnabled())
			Log.trace("Resolving path '{}' in '{}'", path, base);
		Path p;
		if(path.toString().startsWith(base.toString()))
			path = path.substring(base.toString().length());
		else  if(!path.equals(""))
			throw new IllegalStateException(String.format("Path '%s' requested is not a child of '%s'", path, base));
		if(path.startsWith("/")) {
			path = path.substring(1);
		}
		if(path.equals("")) {
			p = base;
		}
		else {
			p = base.resolve(path);
		}
		if(Log.isTraceEnabled())
			Log.trace("Resolved path '{}' as '{}' in '{}'", path, p, base);
		return new PathFile(p, this);
	}

	@Override
	public Event populateEvent(Event evt) {
		return evt;
	}

	@Override
	public PathFile getDefaultPath() throws PermissionDeniedException, IOException {
		return getFile("/");
	}
}
