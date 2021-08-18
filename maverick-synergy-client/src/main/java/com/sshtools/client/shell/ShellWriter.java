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