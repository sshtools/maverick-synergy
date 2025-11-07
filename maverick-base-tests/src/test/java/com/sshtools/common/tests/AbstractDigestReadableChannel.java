package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
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

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class AbstractDigestReadableChannel implements ReadableByteChannel {

	private boolean open = true;
	protected final MessageDigest digest;

	protected AbstractDigestReadableChannel()
			throws NoSuchAlgorithmException {
		this.digest = MessageDigest.getInstance("MD5");
	}

	public final byte[] digest() {
		return digest.digest();
	}

	@Override
	public final boolean isOpen() {
		return open;
	}

	@Override
	public final void close() throws IOException {
		open  = false;
		onClose();
	}
	
	protected void onClose() throws IOException {
	}

	protected void checkClosed() throws ClosedChannelException {
		if(!open)
			throw new ClosedChannelException();
	}

}
