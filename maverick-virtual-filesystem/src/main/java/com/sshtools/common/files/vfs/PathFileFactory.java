
package com.sshtools.common.files.vfs;

import java.io.IOException;
import java.nio.file.Path;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;

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
		else 
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
