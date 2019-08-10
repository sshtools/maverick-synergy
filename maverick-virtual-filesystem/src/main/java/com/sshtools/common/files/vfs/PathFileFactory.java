package com.sshtools.common.files.vfs;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public class PathFileFactory implements AbstractFileFactory<PathFile> {
	final static Logger LOG = LoggerFactory.getLogger(PathFileFactory.class);
	
	private Path base;

	public PathFileFactory(Path base) {
		this.base = base;
	}

	@Override
	public PathFile getFile(String path, SshConnection con) throws PermissionDeniedException, IOException {
		if(LOG.isTraceEnabled())
			LOG.trace(String.format("Resolving path '%s' in '%s'", path, base));
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
		if(LOG.isTraceEnabled())
			LOG.trace(String.format("Resolved path '%s' as '%s' in '%s'", path, p, base));
		return new PathFile(p, this);
	}

	@Override
	public Event populateEvent(Event evt) {
		return evt;
	}

	@Override
	public PathFile getDefaultPath(SshConnection con) throws PermissionDeniedException, IOException {
		return getFile("/", con);
	}
}
