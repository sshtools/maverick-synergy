package com.sshtools.client.shell;

import java.io.IOException;

public interface ShellWriter {

	/**
	 * Interrupt the process by sending a Ctrl+C to the process.
	 *
	 * @throws IOException
	 */
	public abstract void interrupt() throws IOException;

	/**
	 * Send data to the remote command just like the user had typed it.
	 * @param string the typed key data
	 * @throws IOException
	 */
	public abstract void type(String string) throws IOException;

	/**
	 * Send a carriage return to the remote command.
	 * @throws IOException
	 */
	public abstract void carriageReturn() throws IOException;

	/**
	 * Send data to the remote command and finish with a carriage return.
	 * @param string String
	 * @throws IOException
	 */
	public abstract void typeAndReturn(String string) throws IOException;

}