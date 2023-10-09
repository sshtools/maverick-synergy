package com.sshtools.client.shell;

import java.io.IOException;

import com.sshtools.common.ssh.SshException;

public interface ShellReader {

	/**
	 * Read a line of output from the process.
	 * 
	 * @return
	 * @throws IOException
	 */
	public abstract String readLine() throws SshException,
			ShellTimeoutException;

	/**
	 * Read a line of output from the process.
	 * 
	 * @param timeout
	 * @return
	 * @throws IOException
	 */
	public abstract String readLine(long timeout) throws SshException,
			ShellTimeoutException;

}