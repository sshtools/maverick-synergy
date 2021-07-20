
package com.sshtools.server.vsession;

public interface MshListener {

	public default void commandStarted(Command cmd, String[] args, VirtualConsole console) { };

	public default void commandFinished(Command cmd, String[] args, VirtualConsole console) { };

	public default void finished(String[] args, VirtualConsole console) { };

	public default void started(String[] args, VirtualConsole console) { };
}
