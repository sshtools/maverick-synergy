package com.sshtools.common.ssh.components;

import com.sshtools.common.ssh.SecurityLevel;

public interface SecurityManager {

	SecurityLevel getSecurityLevel(String algorithm);
}
