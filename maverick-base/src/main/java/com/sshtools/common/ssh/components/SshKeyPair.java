package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import com.sshtools.common.publickey.SignatureGenerator;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * Storage class for a public/private key pair.
 * @author Lee David Painter
 */
public class SshKeyPair implements SignatureGenerator {
  SshPrivateKey privatekey;
  SshPublicKey publickey;

  /**
   * Get the private key.
   * @return SshPrivateKey
   */
  public SshPrivateKey getPrivateKey() {
    return privatekey;
  }

  /**
   * Get the public key.
   * @return SshPublicKey
   */
  public SshPublicKey getPublicKey() {
    return publickey;
  }

  /**
   * Wraps a public/private key pair into an SshKeyPair instance.
   *
   * @param prv
   * @param pub
   * @return SshKeyPair
   */
  public static SshKeyPair getKeyPair(SshPrivateKey prv, SshPublicKey pub) {
    SshKeyPair pair = new SshKeyPair();
    pair.publickey = pub;
    pair.privatekey = prv;
    return pair;
  }

  /**
   * Set the private key
   * 
   * @param privatekey
   */
  public void setPrivateKey(SshPrivateKey privatekey) {
      this.privatekey = privatekey;
    
  }

  /**
   * Set the public key
   * 
   * @param publickey
   */
  public void setPublicKey(SshPublicKey publickey) {
      this.publickey = publickey;
    
  }

	@Override
	public int hashCode() {
		return privatekey.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SshKeyPair) {
			SshKeyPair other = (SshKeyPair)obj;
			if(other.privatekey!=null && other.publickey!=null) {
				if(privatekey!=null && publickey !=null) {
					return other.privatekey.equals(privatekey) && other.publickey.equals(publickey);
				}
			}
		}
		return false;
	}

	@Override
	public byte[] sign(SshPublicKey key, String signingAlgorithm, byte[] data) throws SshException, IOException {
		
		ByteArrayWriter sig = new ByteArrayWriter();

		try {
			byte[] s = getPrivateKey().sign(data, signingAlgorithm);
			sig.writeString(signingAlgorithm);
			sig.writeBinaryString(s);
			return sig.toByteArray();
		} finally {
			sig.close();
		}
	}

	@Override
	public Collection<SshPublicKey> getPublicKeys() throws IOException {
		return Arrays.asList(getPublicKey());
	}

  
}
