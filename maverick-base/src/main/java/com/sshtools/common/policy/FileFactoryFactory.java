package com.sshtools.common.policy;

import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.ssh.SshConnection;

public interface FileFactoryFactory {

	AbstractFileFactory<?> createFileFactory(SshConnection con);
}
