package com.maverick.agent.rfc;

import java.io.IOException;

import com.maverick.agent.AgentMessage;
import com.maverick.agent.exceptions.InvalidMessageException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshAgentFailure extends AgentMessage {
    
    private int errorcode;

    /**
     * Creates a new SshAgentFailure object.
     */
    public SshAgentFailure() {
        super(RFCAgentMessages.SSH_AGENT_FAILURE);
    }

    /**
     * Creates a new SshAgentFailure object.
     *
     * @param errorcode
     */
    public SshAgentFailure(int errorcode) {
        super(RFCAgentMessages.SSH_AGENT_FAILURE);
        this.errorcode = errorcode;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_AGENT_FAILURE";
    }

    /**
     *
     *
     * @return
     */
    public int getErrorCode() {
        return errorcode;
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
            baw.writeInt(errorcode);
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
        	if(bar.available() > 0) {
        		errorcode = (int) bar.readInt();
        	}
        } catch (IOException ioe) {
            throw new InvalidMessageException(ioe.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
