
package com.sshtools.common.publickey;

import java.io.IOException;

import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * Interface which all public key formats must implement to provide decoding of
 * the public key into a suitable format for the API.
 * 
 * @author Lee David Painter
 */
public interface SshPublicKeyFile {

	/**
	 * Convert the key file into a usable <a
	 * href="../../maverick/ssh/SshPublicKey.html"> SshPublicKey</a>.
	 * 
	 * @return SshPublicKey
	 * @throws IOException
	 */
	public SshPublicKey toPublicKey() throws IOException;

	/**
	 * Get the comment applied to the key file.
	 * 
	 * @return String
	 */
	public String getComment();

	/**
	 * Get the formatted key.
	 * 
	 * @return byte[]
	 * @throws IOException
	 */
	public byte[] getFormattedKey() throws IOException;

	/**
	 * Get the options string applied to the key file
	 * 
	 * @return options string
	 */
	public String getOptions();

}
