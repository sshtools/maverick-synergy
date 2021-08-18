/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

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
