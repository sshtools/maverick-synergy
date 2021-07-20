
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
