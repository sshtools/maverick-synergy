/* HEADER */
package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.security.PrivateKey;

import com.sshtools.common.ssh.SshException;

/**
 *  Interface for SSH supported private keys.
 *
 *  @author Lee David Painter
 */
public interface SshPrivateKey {

  /**
   * Create a signature from the data.
   * @param data
   * @return byte[]
   * @throws SshException
   */
  public byte[] sign(byte[] data) throws IOException;

  public byte[] sign(byte[] data, String signingAlgorithm) throws IOException;
  
  public String getAlgorithm();

  PrivateKey getJCEPrivateKey();
}
