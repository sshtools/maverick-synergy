package com.sshtools.server.vsession;

public interface MshListener {

	public void commandStarted(Command cmd, String[] args, VirtualConsole console);

	public void commandFinished(Command cmd, String[] args, VirtualConsole console);
}
