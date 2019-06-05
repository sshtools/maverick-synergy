package com.sshtools.common.ssh;

import java.util.Collection;

public interface SshConnectionManager {

	void setupConnection(SshConnection con);

	void clearConnection();

	Collection<SshConnection> getAllConnections();

	SshConnection getConnectionById(String uuid);

}
