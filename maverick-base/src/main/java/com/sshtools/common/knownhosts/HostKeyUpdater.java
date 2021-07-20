
package com.sshtools.common.knownhosts;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public interface HostKeyUpdater {

	boolean isKnownHost(String host, SshPublicKey key) throws SshException;
	
	void updateHostKey(String host, SshPublicKey key) throws SshException;
}
