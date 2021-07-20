
package com.sshtools.agent.rfc;


import java.io.IOException;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.KeyConstraints;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshAgentAddKey extends AgentMessage {
   
    SshPrivateKey prvkey;
    SshPublicKey pubkey;
    String description;
    KeyConstraints constraints;

    /**
     * Creates a new SshAgentAddKey object.
     */
    public SshAgentAddKey() {
        super(RFCAgentMessages.SSH_AGENT_ADD_KEY);
    }

    /**
     * Creates a new SshAgentAddKey object.
     *
     * @param prvkey
     * @param pubkey
     * @param description
     * @param constraints
     */
    public SshAgentAddKey(SshPrivateKey prvkey, SshPublicKey pubkey,
        String description, KeyConstraints constraints) {
        super(RFCAgentMessages.SSH_AGENT_ADD_KEY);
        this.prvkey = prvkey;
        this.pubkey = pubkey;
        this.description = description;
        this.constraints = constraints;
    }

    /**
     *
     *
     * @return
     */
    public SshPrivateKey getPrivateKey() {
        return prvkey;
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
    public KeyConstraints getKeyConstraints() {
        return constraints;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_AGENT_ADD_KEY";
    }

    /**
     *
     *
     * @param baw
     *
     * @throws java.io.IOException
     * @throws com.maverick.agent.exceptions.j2ssh.transport.InvalidMessageException DOCUMENT
     *         ME!
     * @throws InvalidMessageException
     */
    public void constructByteArray(ByteArrayWriter baw)
        throws java.io.IOException, 
            InvalidMessageException {
        try {
        	baw.write(RFCAgentMessages.SSH_AGENT_ADD_KEY);
        	SshKeyPair keyPair=new SshKeyPair();
        	keyPair.setPrivateKey(prvkey);
        	keyPair.setPublicKey(pubkey);
        	baw.writeBinaryString(SshPrivateKeyFileFactory.create(keyPair,null,null).getFormattedKey()); //changed by Aruna, need to check with Lee
            baw.writeBinaryString(pubkey.getEncoded());
            baw.writeString(description);
            baw.write(constraints.toByteArray());
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
     * @throws com.maverick.agent.exceptions.j2ssh.transport.InvalidMessageException DOCUMENT
     *         ME!
     * @throws InvalidMessageException
     */
    public void constructMessage(ByteArrayReader bar)
        throws java.io.IOException, 
            InvalidMessageException {
        try {
        	bar.read();
        	SshKeyPair keyPair=SshPrivateKeyFileFactory.parse(bar.readBinaryString()).toKeyPair(null);
        	bar.readBinaryString();
        	
            prvkey =keyPair.getPrivateKey();
            pubkey = keyPair.getPublicKey();
            description = bar.readString();
            constraints = new KeyConstraints(bar);
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(), SshException.AGENT_ERROR);
        } catch (InvalidPassphraseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
