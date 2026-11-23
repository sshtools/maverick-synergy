package com.sshtools.common.files.memory;

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

public class InMemoryFileRandomAccessTest {

	@Test
	public void rejectsSparseWriteBeyondConfiguredMaximum() throws Exception {
		InMemoryFileFactory factory = new InMemoryFileFactory("/", 16);
		InMemoryFile file = factory.getFile("bounded");
		try (AbstractCloseableRandomAccess access = new AbstractCloseableRandomAccess(
				file.openFile(true))) {
			access.randomAccess.seek(16);
			assertThrows(IOException.class, () -> access.randomAccess.write(new byte[1], 0, 1));
		}
	}

	@Test
	public void permitsWriteAtMaximumExtent() throws Exception {
		InMemoryFileFactory factory = new InMemoryFileFactory("/", 16);
		InMemoryFile file = factory.getFile("bounded");
		try (AbstractCloseableRandomAccess access = new AbstractCloseableRandomAccess(
				file.openFile(true))) {
			access.randomAccess.seek(15);
			access.randomAccess.write(new byte[] { 42 }, 0, 1);
		}
		assertEquals(16, file.length());
	}

	@Test
	public void rejectsOversizedOutputStreamWrite() throws Exception {
		InMemoryFileFactory factory = new InMemoryFileFactory("/", 16);
		InMemoryFile file = factory.getFile("bounded");
		try (java.io.OutputStream output = file.getOutputStream()) {
			assertThrows(IOException.class, () -> output.write(new byte[17], 0, 17));
		}
	}

	private static final class AbstractCloseableRandomAccess
			implements java.io.Closeable {
		private final com.sshtools.common.files.AbstractFileRandomAccess randomAccess;

		private AbstractCloseableRandomAccess(
				com.sshtools.common.files.AbstractFileRandomAccess randomAccess) {
			this.randomAccess = randomAccess;
		}

		@Override
		public void close() throws IOException {
			randomAccess.close();
		}
	}
}
