/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */


package com.sshtools.client.shell;

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

	public String getCommandOutput() {
		return in.getCommandOutput();
	}

	public ExpectShell getShell() {
		return shell;
	}
	
	public ShellProcess drain() throws IOException {
		while(in.isActive() && in.read() > -1);
		return this;
	}


}
