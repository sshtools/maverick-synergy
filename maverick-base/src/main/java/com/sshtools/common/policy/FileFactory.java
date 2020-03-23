package com.sshtools.common.policy;

import java.io.IOException;

import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.ssh.SshConnection;

public interface FileFactory {

	AbstractFileFactory<?> getFileFactory(SshConnection con) throws IOException;
}
