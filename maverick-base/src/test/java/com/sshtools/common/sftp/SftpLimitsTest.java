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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;

import org.junit.Test;

public class SftpLimitsTest {

	@Test
	public void acceptsMaximumReadLength() throws IOException {
		int maximumPacketSize = 131328;
		assertEquals(maximumPacketSize - SftpLimits.READ_RESPONSE_OVERHEAD,
				SftpLimits.checkedDataLength(
						maximumPacketSize - SftpLimits.READ_RESPONSE_OVERHEAD,
						maximumPacketSize, "READ"));
	}

	@Test
	public void rejectsReadAboveMaximumBeforeAllocation() {
		assertThrows(IOException.class, () -> SftpLimits.checkedDataLength(
				Integer.MAX_VALUE, 131328, "READ"));
	}

	@Test
	public void rejectsWrappedWriteCount() {
		assertThrows(IOException.class, () -> SftpLimits.checkedWriteLength(
				0x80000000L, 131072, 131315, "WRITE"));
	}

	@Test
	public void requiresDeclaredWritePayload() {
		assertThrows(IOException.class, () -> SftpLimits.checkedWriteLength(
				10, 9, 131315, "WRITE"));
	}
}
