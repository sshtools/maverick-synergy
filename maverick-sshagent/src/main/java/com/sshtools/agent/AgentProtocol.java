package com.sshtools.agent;

/*-
 * #%L
 * Key Agent
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.sshtools.common.util.ByteArrayReader;

public final class AgentProtocol {

	public static final int MAX_MESSAGE_LENGTH = 1024 * 1024;
	public static final int MAX_RANDOM_BYTES = 64 * 1024;

	private AgentProtocol() {
	}

	public static byte[] readMessage(InputStream input) throws IOException {
		byte[] encodedLength = new byte[4];
		readFully(input, encodedLength);
		long length = ByteArrayReader.readInt(encodedLength, 0);
		return readMessage(input, length);
	}

	static byte[] readMessage(InputStream input, long length) throws IOException {
		if (length < 1 || length > MAX_MESSAGE_LENGTH) {
			throw new IOException("SSH agent message length " + length
					+ " is outside the supported range 1.." + MAX_MESSAGE_LENGTH);
		}
		byte[] message = new byte[(int) length];
		readFully(input, message);
		return message;
	}

	public static int validateRandomBytes(int length) throws IOException {
		if (length < 1 || length > MAX_RANDOM_BYTES) {
			throw new IOException("SSH agent random-data request length " + length
					+ " is outside the supported range 1.." + MAX_RANDOM_BYTES);
		}
		return length;
	}

	private static void readFully(InputStream input, byte[] buffer) throws IOException {
		int position = 0;
		while (position < buffer.length) {
			int read = input.read(buffer, position, buffer.length - position);
			if (read < 0) {
				throw new EOFException("SSH agent connection closed after " + position
						+ " of " + buffer.length + " bytes");
			}
			if (read > 0) {
				position += read;
			} else {
				int next = input.read();
				if (next < 0) {
					throw new EOFException("SSH agent connection closed after " + position
							+ " of " + buffer.length + " bytes");
				}
				buffer[position++] = (byte) next;
			}
		}
	}
}
