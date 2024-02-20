package com.sshtools.common.files.direct;

import java.io.File;
import java.io.IOException;

import com.sshtools.common.files.AbstractFileFactory;

/**
 * Deprecated, use {@link NioFile} and {@link NioFileFactory} instead.
 */
@Deprecated(since = "3.1.0", forRemoval = true)
public class DirectFileJava7 extends DirectFile {

	public DirectFileJava7(String path, AbstractFileFactory<DirectFile> fileFactory, File homeDir) throws IOException {
		super(path, fileFactory, homeDir);
	}
	
}
