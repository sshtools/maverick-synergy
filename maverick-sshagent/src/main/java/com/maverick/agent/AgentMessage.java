package com.maverick.agent;

import java.io.IOException;

import com.maverick.agent.exceptions.InvalidMessageException;
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
