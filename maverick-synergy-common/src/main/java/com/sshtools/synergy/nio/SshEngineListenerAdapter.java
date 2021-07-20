
package com.sshtools.synergy.nio;

import java.io.IOException;

public class SshEngineListenerAdapter implements SshEngineListener {

	@Override
	public void interfaceStarted(SshEngine engine, ListeningInterface li) {
	}

	@Override
	public void interfaceStopped(SshEngine engine, ListeningInterface li) {
	}

	@Override
	public void interfaceCannotStart(SshEngine engine, ListeningInterface li, IOException ex) {
	}

	@Override
	public void interfaceCannotStop(SshEngine engine, ListeningInterface li, IOException e) {
	}

	@Override
	public void starting(SshEngine engine) {
	}

	@Override
	public void started(SshEngine engine) {
	}

	@Override
	public void shuttingDown(SshEngine engine) {
	}

	@Override
	public void shutdown(SshEngine engine) {
	}

}
