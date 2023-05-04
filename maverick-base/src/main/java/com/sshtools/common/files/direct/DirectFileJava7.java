package com.sshtools.common.files.direct;

import java.io.File;
import java.io.IOException;

import com.sshtools.common.files.AbstractFileFactory;

/**
 * Deprecated for removal. Just use {@link DirectFile}.
 */
@Deprecated(since = "3.1.0")
public class DirectFileJava7 extends DirectFile {

	public DirectFileJava7(String path, AbstractFileFactory<DirectFile> fileFactory, File homeDir) throws IOException {
		super(path, fileFactory, homeDir);
	}
	
}
