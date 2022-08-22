/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */


package com.sshtools.common.ssh.components;

import java.security.PublicKey;

import com.sshtools.common.ssh.SecureComponent;
import com.sshtools.common.ssh.SshException;


/**
 * <p>Interface for SSH supported public keys.</p>
 * @author Lee David Painter
 */
public interface SshPublicKey extends SecureComponent {
	
   /**
   * Initialize the public key from a blob of binary data.
   * @param blob
   * @param start
   * @param len
   * @throws SshException
   */
  public void init(byte[] blob, int start, int len) throws SshException;

  /**
   * Get the algorithm name for the public key.
   * @return the algorithm name, for example "ssh-dss"
   */
  public String getAlgorithm();

  
  /**
   * The algorithm name expected to be encoded in SSH signatures
   */
  public String getSigningAlgorithm();
  
  
  /**
   * The algorithm name used in the encoding of the public key
   */
  public String getEncodingAlgorithm();
  
  /**
   * Get the bit length of the public key
   * @return the bit length of the public key
   */
  public int getBitLength();

  /**
       * Encode the public key into a blob of binary data, the encoded result will be
   * passed into init to recreate the key.
   *
   * @return an encoded byte array
   * @throws SshException
   */
  public byte[] getEncoded() throws SshException;

  /**
   * Return an SSH fingerprint of the public key
   * @return String
   * @throws SshException
   */
  public String getFingerprint() throws SshException;

  /**
   * Verify the signature.
   * @param signature
   * @param data
   * @return <code>true</code> if the signature was produced by the corresponding
   * private key that owns this public key, otherwise <code>false</code>.
   * @throws SshException
   */
  public boolean verifySignature(byte[] signature,
                                 byte[] data)
     throws SshException;
  
  /**
   * Test the JCE for algorithm availability.
 * @return 
   */
  public String test();

  /**
   * Return the JCE component for this key.
   * @return
   */
  public PublicKey getJCEPublicKey();

  /**
   * Indicates if this key is part of a certificate.
   * @return
   */
  public default boolean isCertificate() {
	return false;
}
}
