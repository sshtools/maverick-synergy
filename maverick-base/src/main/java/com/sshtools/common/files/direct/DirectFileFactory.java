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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.files.direct;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileHomeFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

@SuppressWarnings({ "unchecked", "static-access" })
public class DirectFileFactory extends AbstractDirectFileFactory<DirectFile> {

	
	
	File defaultPath = new File(".");
	static Class<DirectFile> clz = null;
	static Constructor<DirectFile> constructor = null;
	static {
		
		try {
			clz = (Class<DirectFile>)DirectFileFactory.class.forName("com.sshtools.common.files.direct.DirectFileJava7");
			constructor = clz.getConstructor(String.class, AbstractFileFactory.class, SshConnection.class, String.class);
		} catch (Throwable e) {
			Log.warn("Falling back to simple DirectFile implementation as current version of Java does not appear to support Path and FileAttributes APIs");
		}
	}
	public DirectFileFactory() {
		super(new DirectFileHomeFactory());
	}
	
	public DirectFileFactory(AbstractFileHomeFactory homeFactory) {
		super(homeFactory);
	}
	
	public DirectFile getFile(String path, SshConnection con)
			throws PermissionDeniedException, IOException {
		
		if(constructor!=null) {
			try {
				return constructor.newInstance(path, this, con, homeFactory.getHomeDirectory(con));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		} 
		
		return new DirectFile(path, this, con, homeFactory.getHomeDirectory(con));
		
	}

	public Event populateEvent(Event evt) {
		return evt;
	}

}
