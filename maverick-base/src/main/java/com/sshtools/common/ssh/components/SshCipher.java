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

import java.io.IOException;

import com.sshtools.common.ssh.SecureComponent;
import com.sshtools.common.ssh.SecurityLevel;

/**
 *
 * <p>Base class for all SSH protocol ciphers. The cipher
 * itself has 2 modes, encryption or decrpytion. The same method is used
 * to transporm the data depending upon the mode.</p>
 *
 * @author Lee David Painter
 */
public abstract class SshCipher implements SshComponent, SecureComponent {

    final String algorithm;
    final SecurityLevel securityLevel;
    final int priority;
    
    public SshCipher(String algorithm, SecurityLevel securityLevel, int priority) {
        this.algorithm = algorithm;
        this.securityLevel = securityLevel;
        this.priority = priority;
    }

    public SecurityLevel getSecurityLevel() {
    	return securityLevel;
    }
    
    public int getPriority() {
    	return priority;
    }
    
    public String getAlgorithm() {
        return algorithm;
    }

  /**
   * Encryption mode.
   */
  public static final int ENCRYPT_MODE = 0;

  /**
   * Decryption mode.
   */
  public static final int DECRYPT_MODE = 1;

  /**
   * Get the cipher block size.
   *
   * @return the block size in bytes.
   */
  public abstract int getBlockSize();

  /**
   * Return the key length required
   * @return
   */
  public abstract int getKeyLength();
  
  /**
   * Initialize the cipher with up to 40 bytes of iv and key data. Each implementation
   * should take as much data from the initialization as it needs ignoring any data
   * that it does not require.
   *
   * @param mode     the mode to operate
   * @param iv       the initiaization vector
   * @param keydata  the key data
   * @throws IOException
   */
  public abstract void init(int mode, byte[] iv, byte[] keydata) throws
      IOException;

  /**
   * Transform the byte array according to the cipher mode.
   * @param data
   * @throws IOException
   */
  public void transform(byte[] data) throws IOException {
    transform(data, 0, data, 0, data.length);
  }

  /**
   * Transform the byte array according to the cipher mode; it is legal for the
   * source and destination arrays to reference the same physical array so
   * care should be taken in the transformation process to safeguard this rule.
   *
   * @param src
   * @param start
   * @param dest
   * @param offset
   * @param len
   * @throws IOException
   */
  public abstract void transform(byte[] src, int start, byte[] dest, int offset,
                                 int len) throws
      IOException;


   public boolean isMAC() {
	return false;
   }
   
   public int getMacLength() {
	   return 0;
   }


   public abstract String getProviderName();

}
