package com.sshtools.agent.rfc;

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

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshAgentRandomData extends AgentMessage {
    
    private byte[] data;

    /**
     * Creates a new SshAgentRandomData object.
     */
    public SshAgentRandomData() {
        super(RFCAgentMessages.SSH_AGENT_RANDOM_DATA);
    }

    /**
     * Creates a new SshAgentRandomData object.
     *
     * @param data
     */
    public SshAgentRandomData(byte[] data) {
        super(RFCAgentMessages.SSH_AGENT_RANDOM_DATA);
        this.data = data;
    }

    /**
     *
     *
     * @return
     */
    public byte[] getRandomData() {
        return data;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_AGENT_RANDOM_DATA";
    }

    /**
     *
     *
     * @param baw
     *
     * @throws java.io.IOException
     * @throws com.sshtools.j2ssh.transport.InvalidMessageException DOCUMENT
     *         ME!
     * @throws InvalidMessageException
     */
    public void constructByteArray(ByteArrayWriter baw)
        throws java.io.IOException, 
            InvalidMessageException {
        try {
            baw.writeBinaryString(data);
        } catch (IOException ioe) {
            throw new InvalidMessageException(ioe.getMessage(),SshException.AGENT_ERROR);
        }
    }

    /**
     *
     *
     * @param bar
     *
     * @throws java.io.IOException
     * @throws com.sshtools.j2ssh.transport.InvalidMessageException DOCUMENT
     *         ME!
     * @throws InvalidMessageException
     */
    public void constructMessage(ByteArrayReader bar)
        throws java.io.IOException, 
            InvalidMessageException {
        try {
            data = bar.readBinaryString();
        } catch (IOException ioe) {
            throw new InvalidMessageException(ioe.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
