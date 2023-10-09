package com.sshtools.common.files.direct;

import com.sshtools.common.ssh.SshConnection;

public class DirectFileHomeFactory {
	public String getHomeDirectory(SshConnection con) {
		String os = System.getProperty("os.name");
		if(os.startsWith("Mac OS X"))
			return "/Users/" + con.getUsername();
		else if(os.startsWith("Windows 1"))
			return "/Users/" + con.getUsername();
		else if(os.startsWith("Windows"))
			return "/Documents and Settings/" + con.getUsername();
		else
			return "/home/" + con.getUsername();
	}
}