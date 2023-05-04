package com.sshtools.common.sftp;

import java.util.regex.Pattern;

public class RegexSftpFileFilter implements SftpFileFilter {

	Pattern p;

	public RegexSftpFileFilter(String filter) {
		this.p = Pattern.compile(filter);
	}

	@Override
	public boolean matches(String name) {
		return p.matcher(name).matches();
	}

}
