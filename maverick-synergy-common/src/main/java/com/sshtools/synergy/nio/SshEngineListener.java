package com.sshtools.synergy.nio;

import java.io.IOException;

public interface SshEngineListener {

	default void interfaceStarted(SshEngine engine, ListeningInterface li) { };

	default void interfaceStopped(SshEngine engine, ListeningInterface li) { };

	default void interfaceCannotStart(SshEngine engine, ListeningInterface li, IOException ex) { };

	default void interfaceCannotStop(SshEngine engine, ListeningInterface li, IOException e) { } ;

	default void starting(SshEngine engine) { };

	default void started(SshEngine engine) { };

	default void shuttingDown(SshEngine engine) { };

	default void shutdown(SshEngine engine) { };
}
