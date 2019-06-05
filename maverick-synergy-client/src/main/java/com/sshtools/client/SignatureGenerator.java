package com.sshtools.client;

import java.io.IOException;
import java.util.Collection;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * Provides a callback when a private key signature is required. This
 * is suitable for use when you do not
 * have direct access to the private key, but know its public key and
 * have access to some mechanism that enables you to request a signature
 * from the corresponding private key (such as an ssh agent).
 */
public interface SignatureGenerator {

  /**
   * Sign the data using the private key of the public key provided.
   * @param key
   * @param data
   * @return byte[]
   * @throws IOException
   */
  public byte[] sign(SshPublicKey key, byte[] data) throws SshException;
  
  public Collection<SshPublicKey> getPublicKeys();
  
}
