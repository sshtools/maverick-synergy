package com.sshtools.agent;

/*-
 * #%L
 * Key Agent
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

import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * Super class of all client messages
 * @author Aruna Abesekara
 *
 */
public class AgentMessage {

	private int type;

	public AgentMessage(int type) {
		this.type = type;

	}

	public String getMessageName() {
		return  String.valueOf(type);
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	public int getMessageType() {
		return type;
	}

	/**
	 * 
	 * 
	 * @param baw
	 * 
	 * @throws InvalidMessageException
	 * @throws IOException
	 */
	public void constructByteArray(ByteArrayWriter baw)
			throws InvalidMessageException, IOException {
	}

	/**
	 * 
	 * 
	 * @param bar
	 * 
	 * @throws InvalidMessageException
	 * @throws IOException
	 */
	public void constructMessage(ByteArrayReader bar)
			throws InvalidMessageException, IOException {
	}

	/**
	 * 
	 * 
	 * @param data
	 * 
	 * @throws InvalidMessageException
	 */
	public void fromByteArray(byte[] data) throws InvalidMessageException {
		
		ByteArrayReader bar = new ByteArrayReader(data);
		
		try {	

			if (bar.available() > 0) {
				type = bar.read();
				constructMessage(bar);
			} else {
				throw new InvalidMessageException(
						"Not enough message data to complete the message",
						SshException.AGENT_ERROR);
			}
		} catch (IOException ioe) {
			throw new InvalidMessageException(
					"The message data cannot be read!",
					SshException.AGENT_ERROR);
		} finally {
			bar.close();
		}
	}

	/**
	 * 
	 * 
	 * @return
	 * 
	 * @throws InvalidMessageException
	 */
	public byte[] toByteArray() throws InvalidMessageException {
		try {
			ByteArrayWriter baw = new ByteArrayWriter();
			baw.write(type);
			constructByteArray(baw);

			return baw.toByteArray();
		} catch (IOException ioe) {
			throw new InvalidMessageException(
					"The message data cannot be written!",
					SshException.AGENT_ERROR);
		}
	}
}
