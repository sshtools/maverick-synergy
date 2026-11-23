package com.sshtools.common.sftp;

/*-
 * #%L
 * Base API
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

import java.io.IOException;

public final class SftpLimits {

	public static final int READ_RESPONSE_OVERHEAD = 13;
	public static final int MAX_DIRECTORY_ENTRIES = 10000;
	public static final int MAX_DIRECTORY_HANDLES = 50;
	public static final int MAX_OUTSTANDING_OPERATIONS = 256;
	public static final int MAX_QUEUED_BYTES = 16 * 1024 * 1024;
	public static final long MAX_EXTENSION_WORK_BYTES = 256L * 1024 * 1024;

	private SftpLimits() {
	}

	public static int maximumPacketSize(int transportMaximum, int remoteChannelPacket) {
		int maximum = transportMaximum;
		if (remoteChannelPacket > 0) {
			maximum = Math.min(maximum, remoteChannelPacket);
		}
		return maximum;
	}

	public static int checkedDataLength(long length, int maximumPacketSize, String operation)
			throws IOException {
		long maximum = maximumPacketSize - READ_RESPONSE_OVERHEAD;
		if (length < 1 || length > maximum) {
			throw new IOException(operation + " length " + length
					+ " is outside the supported range 1.." + Math.max(0, maximum));
		}
		return (int) length;
	}

	public static int checkedWriteLength(long length, int available, int maximumDataLength,
			String operation) throws IOException {
		int checked = checkedDataLength(length, maximumDataLength + READ_RESPONSE_OVERHEAD,
				operation);
		if (checked > available) {
			throw new IOException(operation + " length " + checked
					+ " exceeds the remaining request payload of " + available);
		}
		return checked;
	}
}
