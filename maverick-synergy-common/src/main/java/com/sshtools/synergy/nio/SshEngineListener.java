
package com.sshtools.synergy.nio;

import java.io.IOException;

public interface SshEngineListener {

	void interfaceStarted(SshEngine engine, ListeningInterface li);

	void interfaceStopped(SshEngine engine, ListeningInterface li);

	void interfaceCannotStart(SshEngine engine, ListeningInterface li, IOException ex);

	void interfaceCannotStop(SshEngine engine, ListeningInterface li, IOException e);

	void starting(SshEngine engine);

	void started(SshEngine engine);

	void shuttingDown(SshEngine engine);

	void shutdown(SshEngine engine);
}
