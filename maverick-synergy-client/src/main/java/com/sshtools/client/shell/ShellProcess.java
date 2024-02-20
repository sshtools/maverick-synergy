package com.sshtools.client.shell;

/*-
 * #%L
 * Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.common.ssh.SshIOException;

public class ShellProcess {

	ExpectShell shell;
	ShellInputStream in;
	BufferedInputStream bin;
	ShellProcess(ExpectShell shell, ShellInputStream in) {
		this.shell = shell;
		this.in = in;
		this.bin = new BufferedInputStream(in);
	}

	public void mark(int readlimit) {
		bin.mark(readlimit);
	}
	
	public void reset() throws IOException {
		bin.reset();
	}
	
	/**
	 * Returns an InputStream that will contain only the output from the executed ShellProcess. Use 
	 * this for basic access to command output, if you use the expect methods DO NOT use this stream.
	 * @return
	 */
	public InputStream getInputStream() {
		return bin;
	}

	public OutputStream getOutputStream() throws SshIOException {
		return shell.sessionOut;
	}

	public int getExitCode() {
		return in.getExitCode();
	}

	public boolean hasSucceeded() {
		return in.hasSucceeded();
	}

	public boolean isActive() {
		return in.isActive();
	}
	
	public void clearOutput() {
		in.clearOutput();
	}

	public String getCommandOutput() {
		return in.getCommandOutput();
	}

	public ExpectShell getShell() {
		return shell;
	}
	
	public ShellProcess drain() throws IOException {
		while(in.isActive() && bin.read() > -1 && !shell.isClosed());
		return this;
	}


}
