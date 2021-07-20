
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
