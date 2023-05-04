package com.sshtools.client.sftp;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

public class GlobRegExpMatching implements RegularExpressionMatching {

	@Override
	public SftpFile[] matchFilesWithPattern(SftpFile[] files, String pattern)
			throws SftpStatusException, SshException {
		
		PathMatcher matcher =
			    FileSystems.getDefault().getPathMatcher("glob:" + pattern);
		List<SftpFile> matched = new ArrayList<>();
		
		for(SftpFile file : files) {
			if(matcher.matches(Paths.get(file.filename))) {
				matched.add(file);
			}
		}
		return matched.toArray(new SftpFile[0]);
	}

	@Override
	public String[] matchFileNamesWithPattern(AbstractFile[] files, String pattern)
			throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		
		PathMatcher matcher =
			    FileSystems.getDefault().getPathMatcher("glob:" + pattern);
		List<String> matched = new ArrayList<>();
		
		for(AbstractFile file : files) {
			Path path = Paths.get(file.getName());
			if(matcher.matches(path)) {
				matched.add(file.getAbsolutePath());
			}
		}
		
		return matched.toArray(new String[0]);
	}

}
