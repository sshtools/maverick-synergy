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

package com.sshtools.agent.provider.namedpipes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;

public class NamedPipeClient extends AbstractNamedPipe {

	NamedPipeSession session;
	
	public NamedPipeClient(String pipeName) throws IOException {
		super(pipeName);
		
		HANDLE hPipe = assertValidHandle("CreateFile", Kernel32.INSTANCE.CreateFile(getPath(),
                WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
                0,
                null,
                WinNT.OPEN_EXISTING,
                0,
                null));
		
		this.session = new NamedPipeSession(hPipe);
	}
	
	public InputStream getInputStream() {
		return session.getInputStream();
	}
	
	public OutputStream getOutputStream() {
		return session.getOutputStream();
	}
	
	public void close() throws IOException {
		session.close();
	}
}
