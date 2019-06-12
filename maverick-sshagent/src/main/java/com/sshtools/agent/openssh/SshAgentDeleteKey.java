package com.maverick.agent.openssh;

import java.io.IOException;

import com.maverick.agent.AgentMessage;
import com.maverick.agent.exceptions.InvalidMessageException;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshAgentDeleteKey extends AgentMessage {
    
    SshPublicKey pubkey;
    String description;

    /**
     * Creates a new SshAgentDeleteKey object.
     */
    public SshAgentDeleteKey() {
        super(OpenSSHAgentMessages.SSH2_AGENTC_REMOVE_IDENTITY);
    }

    /**
     * Creates a new SshAgentDeleteKey object.
     *
     * @param pubkey
     * @param description
     */
    public SshAgentDeleteKey(SshPublicKey pubkey, String description) {
        super(OpenSSHAgentMessages.SSH2_AGENTC_REMOVE_IDENTITY);
        this.pubkey = pubkey;
        this.description = description;
    }

    /**
     *
     *
     * @return
     */
    public SshPublicKey getPublicKey() {
        return pubkey;
    }

    /**
     *
     *
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_AGENTC_REMOVE_IDENTITY";
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
        throws java.io.IOException, InvalidMessageException {
        try {
            baw.writeBinaryString(pubkey.getEncoded());
            baw.writeString(description);
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(),SshException.AGENT_ERROR);
        } catch (com.sshtools.common.ssh.SshException e) {
			throw new IOException(e.getMessage(), e);
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
            pubkey =  SshPublicKeyFileFactory.decodeSSH2PublicKey(bar.readBinaryString());
            description = bar.readString();
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
