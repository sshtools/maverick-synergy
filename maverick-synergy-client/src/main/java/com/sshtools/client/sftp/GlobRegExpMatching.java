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
package com.sshtools.client.sftp;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
	public String[] matchFileNamesWithPattern(File[] files, String pattern)
			throws SftpStatusException, SshException {
		
		PathMatcher matcher =
			    FileSystems.getDefault().getPathMatcher("glob:" + pattern);
		List<String> matched = new ArrayList<>();
		
		for(File file : files) {
			if(matcher.matches(file.toPath())) {
				matched.add(file.getName());
			}
		}
		
		return matched.toArray(new String[0]);
	}

}
