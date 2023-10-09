package com.sshtools.common.ssh.components;

import java.io.IOException;

import com.sshtools.common.ssh.SecureComponent;
import com.sshtools.common.ssh.SecurityLevel;

public interface SshCipher extends  SshComponent, SecureComponent {
	
	SecurityLevel getSecurityLevel();

	int getPriority();

	String getAlgorithm();

	/**
	   * Encryption mode.
	   */
	int ENCRYPT_MODE = 0;
	/**
	   * Decryption mode.
	   */
	int DECRYPT_MODE = 1;

	/**
	   * Get the cipher block size.
	   *
	   * @return the block size in bytes.
	   */
	int getBlockSize();

	/**
	   * Return the key length required
	   * @return
	   */
	int getKeyLength();

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
	void init(int mode, byte[] iv, byte[] keydata) throws IOException;

	/**
	   * Transform the byte array according to the cipher mode.
	   * @param data
	   * @throws IOException
	   */
	void transform(byte[] data) throws IOException;

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
	void transform(byte[] src, int start, byte[] dest, int offset, int len) throws IOException;

	boolean isMAC();

	int getMacLength();

	String getProviderName();

}