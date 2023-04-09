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
package com.sshtools.common.sftp;

import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.UnsignedInteger32;

public interface OpenFile {

	AbstractFile getFile();

	UnsignedInteger32 getFlags();

	boolean isTextMode();

	long getFilePointer() throws IOException;

	void seek(long longValue) throws IOException;

	int read(byte[] buf, int start, int numBytesToRead) throws IOException, PermissionDeniedException;

	void write(byte[] data, int off, int len) throws IOException, PermissionDeniedException;

	void close() throws IOException;

	void processEvent(Event evt);

	byte[] getHandle();

}
