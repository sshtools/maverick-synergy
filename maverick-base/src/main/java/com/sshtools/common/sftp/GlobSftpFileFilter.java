package com.sshtools.common.sftp;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

public class GlobSftpFileFilter implements SftpFileFilter {

	PathMatcher matcher;
	
	public GlobSftpFileFilter(String filter) {
		matcher = FileSystems.getDefault().getPathMatcher("glob:" + filter);
	}

	@Override
	public boolean matches(String name) {
		return matcher.matches(Paths.get(name));
	}

}
