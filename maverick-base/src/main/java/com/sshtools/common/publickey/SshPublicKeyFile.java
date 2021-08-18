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
