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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

public class AgentProtocolTest {

	@Test
	public void rejectsOversizedFrameBeforeReadingPayload() {
		assertThrows(IOException.class, () -> AgentProtocol.readMessage(
				new ByteArrayInputStream(new byte[0]), AgentProtocol.MAX_MESSAGE_LENGTH + 1L));
	}

	@Test
	public void acceptsMaximumFrameLength() throws IOException {
		byte[] message = AgentProtocol.readMessage(
				new ByteArrayInputStream(new byte[AgentProtocol.MAX_MESSAGE_LENGTH]),
				AgentProtocol.MAX_MESSAGE_LENGTH);
		assertEquals(AgentProtocol.MAX_MESSAGE_LENGTH, message.length);
	}

	@Test
	public void rejectsOversizedRandomRequest() {
		assertThrows(IOException.class,
				() -> AgentProtocol.validateRandomBytes(AgentProtocol.MAX_RANDOM_BYTES + 1));
	}
}
