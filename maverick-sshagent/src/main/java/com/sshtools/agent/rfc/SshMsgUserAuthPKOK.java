/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.agent.rfc;


import java.io.IOException;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.22 $
 */
public class SshMsgUserAuthPKOK extends AgentMessage {
  
    private String algorithm;
    private byte[] key;

    //private boolean ok;

    /**
     * Creates a new SshMsgUserAuthPKOK object.
     */
    public SshMsgUserAuthPKOK() {
        super(RFCAgentMessages.SSH_MSG_USERAUTH_PK_OK);
    }

    /**
     * Creates a new SshMsgUserAuthPKOK object.
     *
     * @param ok
     * @param algorithm
     * @param key
     */
    public SshMsgUserAuthPKOK( /*boolean ok,*/
        String algorithm, byte[] key) {
        super(RFCAgentMessages.SSH_MSG_USERAUTH_PK_OK);
        this.key = key;
    }

     /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_MSG_USERAUTH_PK_OK";
    }

    /**
     *
     *
     * @param baw
     *
     * @throws InvalidMessageException
     */
    public void constructByteArray(ByteArrayWriter baw)
        throws InvalidMessageException {
        try {
            //baw.write(ok ? 1 : 0);
            baw.writeString(algorithm);
            baw.writeBinaryString(key);
        } catch (IOException ioe) {
            throw new InvalidMessageException("Failed to write message data!",SshException.AGENT_ERROR);
        }
    }

    /**
     *
     *
     * @param bar
     *
     * @throws InvalidMessageException
     */
    public void constructMessage(ByteArrayReader bar)
        throws InvalidMessageException {
        try {
            algorithm = bar.readString();
            key = bar.readBinaryString();
        } catch (IOException ioe) {
            throw new InvalidMessageException("Failed to read message data!",SshException.AGENT_ERROR);
        }
    }
}
