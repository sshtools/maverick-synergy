package com.maverick.agent.openssh;

import java.io.IOException;

import com.maverick.agent.AgentMessage;
import com.maverick.agent.exceptions.InvalidMessageException;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
/**
 * Message for Agent Private key operation
 * @author Aruna Abesekara
 * 
 *
 */

public class OpenSshSignRequest extends AgentMessage {
   
    SshPublicKey pubkey;
    byte[] data;
    int flags;
    
    /**
     * Creates a new SshAgentPrivateKeyOp object.
     */
    public OpenSshSignRequest() {
        super(OpenSSHAgentMessages.SSH2_AGENTC_SIGN_REQUEST);
    }

    /**
     * Creates a new SshAgentPrivateKeyOp object.
     *
     * @param pubkey
     * @param operation
     * @param data
     */
    public OpenSshSignRequest(SshPublicKey pubkey, byte[] data) {
        super(OpenSSHAgentMessages.SSH2_AGENTC_SIGN_REQUEST);
        this.pubkey = pubkey;
        this.data = data;
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
    public byte[] getOperationData() {
        return data;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH2_AGENTC_SIGN_REQUEST";
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
            baw.writeBinaryString(pubkey.getEncoded());
            baw.writeBinaryString(data);
            baw.writeInt(flags);
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(),SshException.AGENT_ERROR);
        } catch (com.sshtools.common.ssh.SshException e) {
        	throw new InvalidMessageException(e.getMessage(),SshException.AGENT_ERROR);
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
            pubkey = SshPublicKeyFileFactory.decodeSSH2PublicKey(bar.readBinaryString());
            data = bar.readBinaryString();
            flags = (int)bar.readInt();
            bar.read(data);
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
