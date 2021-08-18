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


package com.sshtools.common.publickey;

import java.io.IOException;

import com.sshtools.common.ssh.components.SshKeyPair;

/**
 * Interface which all private key formats must implement to provide decoding
 * and decryption of the private key into a suitable format for the API.
 *
 * @author Lee David Painter
 */
public interface SshPrivateKeyFile {

  /**
   * Determine if the private key file is protected by a passphrase.
   * @return <tt>true</tt> if the key file is encrypted with a passphrase, otherwise
   * <tt>false</tt>
   * @throws IOException
   */
  public boolean isPassphraseProtected();

  /**
   * Decode the private key using the users passphrase.
   * @param passphrase the users passphrase
   * @return the key pair stored in this private key file.
   * @throws IOException
   * @throws InvalidPassphraseException
   */
  public SshKeyPair toKeyPair(String passphrase) throws IOException, InvalidPassphraseException;

  /**
   * Method to determine whether the format supports changing of passphrases. This
   * typically would indicate that the format is read-only and that keys cannot
   * be saved into this format.
   * @return boolean
   */
  public boolean supportsPassphraseChange();

  /**
   * Get a description of the format type e.g. "OpenSSH"
   * @return String
   */
  public String getType();

  /**
   * Change the passphrase of the key file.
   * @param oldpassphrase the old passphrase
   * @param newpassprase the new passphrase
   * @throws IOException
   * @throws InvalidPassphraseException
   */
  public void changePassphrase(String oldpassphrase, String newpassprase) throws
      IOException, InvalidPassphraseException;

  /**
   * Get the formatted key
   * @return byte[]
   * @throws IOException
   */
  public byte[] getFormattedKey() throws IOException;

  /**
   * The private key comment (if any).
   * @return
   */
  public String getComment();
}
