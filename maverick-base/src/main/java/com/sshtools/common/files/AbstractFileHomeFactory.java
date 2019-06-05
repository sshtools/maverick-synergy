package com.sshtools.common.files;

import com.sshtools.common.ssh.SshConnection;

public interface AbstractFileHomeFactory {

	String getHomeDirectory(SshConnection con);
}
