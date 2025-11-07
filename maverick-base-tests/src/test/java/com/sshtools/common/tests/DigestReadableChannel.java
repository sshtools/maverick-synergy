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
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.security.NoSuchAlgorithmException;

public class DigestReadableChannel extends AbstractDigestReadableChannel {

	private final ReadableByteChannel delegate;

	DigestReadableChannel(ReadableByteChannel delegate)
			throws NoSuchAlgorithmException {
		this.delegate = delegate;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		checkClosed();
		
		var start  = dst.position();
		var read = delegate.read(dst);
		if(read == -1)
			return -1;

		dst.position(start);
		dst.limit(start + read);
		digest.update(dst);
		
		dst.position(start);
		dst.limit(start + read);
		
		return read;
	}

	@Override
	protected void onClose() throws IOException {
		delegate.close();
	}

}
