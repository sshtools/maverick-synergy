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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client.sftp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

public class RegExpMatching implements RegularExpressionMatching {

	@Override
	public SftpFile[] matchFilesWithPattern(SftpFile[] files, String pattern)
			throws SftpStatusException, SshException {
		
		Pattern p = Pattern.compile(pattern);

		List<SftpFile> matched = new ArrayList<>();
		
		for(SftpFile file : files) {
			if(p.matcher(file.getFilename()).matches()) {
				matched.add(file);
			}
		}
		return matched.toArray(new SftpFile[0]);
	}

	@Override
	public String[] matchFileNamesWithPattern(AbstractFile[] files, String pattern)
			throws SftpStatusException, SshException {
		
		Pattern p = Pattern.compile(pattern);
		
		List<String> matched = new ArrayList<>();
		
		for(AbstractFile file : files) {
			if(p.matcher(file.getName()).matches()) {
				matched.add(file.getName());
			}
		}
		
		return matched.toArray(new String[0]);
	}

}
