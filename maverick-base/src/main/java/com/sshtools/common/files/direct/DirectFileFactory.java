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
import java.lang.reflect.Constructor;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;

@SuppressWarnings({ "unchecked", "static-access" })
public class DirectFileFactory extends AbstractDirectFileFactory<DirectFile> {

	File defaultPath = new File(".");
	static Class<DirectFile> clz = null;
	static Constructor<DirectFile> constructor = null;
	static {
		
		try {
			clz = (Class<DirectFile>)DirectFileFactory.class.forName("com.sshtools.common.files.direct.DirectFileJava7");
			constructor = clz.getConstructor(String.class, AbstractFileFactory.class, File.class);
		} catch (Throwable e) {
			Log.warn("Falling back to simple DirectFile implementation as current version of Java does not appear to support Path and FileAttributes APIs");
		}
	}
	
	public DirectFileFactory(File homeDirectory) {
		super(homeDirectory);
	}
	
	public DirectFile getFile(String path)
			throws PermissionDeniedException, IOException {
		
		if(constructor!=null) {
			try {
				return constructor.newInstance(path, this, homeDirectory);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		} 
		
		return new DirectFile(path, this, homeDirectory);
		
	}

	public Event populateEvent(Event evt) {
		return evt;
	}

}
