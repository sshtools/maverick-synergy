package com.sshtools.common.publickey;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
