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

package com.sshtools.common.files.nio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.IOUtils;

public class AbstractFileSeekableByteChannel implements SeekableByteChannel {

	AbstractFile file;
	File tmpfile;
	SeekableByteChannel tmp;
	
	AbstractFileSeekableByteChannel(AbstractFile file) throws IOException {
		this.file = file;
		tmp = Files.newByteChannel((tmpfile = File.createTempFile("abfs", "tmp")).toPath());
	}
	
	@Override
	public boolean isOpen() {
		return tmp.isOpen();
	}

	@Override
	public void close() throws IOException {
		tmp.close();
		try(OutputStream out = file.getOutputStream()) {
			try(InputStream in = new FileInputStream(tmpfile)) {
				IOUtils.copy(in, out);
			}
		} catch (PermissionDeniedException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return tmp.read(dst);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return tmp.write(src);
	}

	@Override
	public long position() throws IOException {
		return tmp.position();
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		return tmp.position(newPosition);
	}

	@Override
	public long size() throws IOException {
		return tmp.size();
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		return tmp.truncate(size);
	}

}
