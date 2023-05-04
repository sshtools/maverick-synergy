package com.sshtools.agent.openssh;


import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.KeyConstraints;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.rsa.Rsa;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.ECUtils;
import com.sshtools.common.ssh.components.jce.Ssh2DsaPrivateKey;
import com.sshtools.common.ssh.components.jce.Ssh2DsaPublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPrivateKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPrivateCrtKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKey;
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
        super(OpenSSHAgentMessages.SSH2_AGENTC_ADD_IDENTITY);
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
        super(OpenSSHAgentMessages.SSH2_AGENTC_ADD_IDENTITY);
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
        return "SSH_AGENTC_ADD_IDENTITY";
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
        	baw.write(OpenSSHAgentMessages.SSH2_AGENTC_ADD_IDENTITY);
        	encodeKey(baw);
            baw.writeString(description);
            baw.write(constraints.toByteArray());
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(), SshException.AGENT_ERROR);
        } catch (SshException e) {
        	throw new InvalidMessageException(e.getMessage(), SshException.AGENT_ERROR);
		}
    }

    protected void encodeKey(ByteArrayWriter baw) throws IOException, SshException {
    	
    }
    
    protected SshKeyPair decodeKey(ByteArrayReader bar) throws IOException, SshException {
    	
    	SshKeyPair pair = new SshKeyPair();
    	String type = bar.readString();
    	try {
			switch(type) {
			case "ssh-dss":
			{
				BigInteger p = bar.readBigInteger();
				BigInteger q = bar.readBigInteger();
				BigInteger g = bar.readBigInteger();
				BigInteger y = bar.readBigInteger();
				BigInteger x = bar.readBigInteger();
				
				pair.setPrivateKey(new Ssh2DsaPrivateKey(p, q, g, x, y));
				pair.setPublicKey(new Ssh2DsaPublicKey(p, q, g, y));
				break;
			}
			case "ssh-rsa":
			{
				BigInteger n = bar.readBigInteger();
				BigInteger e = bar.readBigInteger();
				BigInteger d = bar.readBigInteger();
				BigInteger iqmp = bar.readBigInteger();
				BigInteger p = bar.readBigInteger();
				BigInteger q = bar.readBigInteger();
				
				pair.setPrivateKey(new Ssh2RsaPrivateCrtKey(n, e, d, p, q,
						Rsa.getPrimeExponent(d, p),
						Rsa.getPrimeExponent(d, q),
						iqmp));
				pair.setPublicKey(new Ssh2RsaPublicKey(n, e));
				break;
			}
			case "ecdsa-sha2-nistp256": {
				String curveName = bar.readString();
				byte[] Q = bar.readBinaryString();
				BigInteger d = bar.readBigInteger();
				
				ECPublicKey pub = ECUtils.decodeKey(Q, curveName);
				pair.setPrivateKey(new Ssh2EcdsaSha2NistPrivateKey(ECUtils.decodePrivateKey(d.toByteArray(), pub), curveName));
				pair.setPublicKey(new Ssh2EcdsaSha2NistPublicKey(pub, curveName));
				break;
			}
			case "ecdsa-sha2-nistp384": {
				break;
			}
			case "ecdsa-sha2-nistp521": {
				break;
			}
			default:
				throw new IOException(String.format("Unsupported key type %s", type));
			}
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException e) {
			throw new IOException(e.getMessage(), e);
		} 
    	
    	return pair;
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
        	SshKeyPair keyPair = decodeKey(bar);
        	prvkey = keyPair.getPrivateKey();
        	pubkey = keyPair.getPublicKey();
            description = bar.readString();
            constraints = new KeyConstraints(bar);
        } catch (IOException | SshException ex) {
            throw new InvalidMessageException(ex.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
