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

package com.sshtools.common.files.direct;

import java.io.File;
import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.permissions.PermissionDeniedException;

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
