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
package com.sshtools.common.publickey.putty;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyProvider;

public class PuTTYPrivateKeyProvider implements SshPrivateKeyProvider {

	@Override
	public boolean isFormatted(byte[] formattedkey) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(formattedkey)));
		try {
			String line = reader.readLine();

			return (line != null && (line.startsWith("PuTTY-User-Key-File-3:")
					|| line.startsWith("PuTTY-User-Key-File-2:") || line.equals("PuTTY-User-Key-File-1:")));
		} catch (IOException ex) {
			return false;
		}
	}

	@Override
	public SshPrivateKeyFile create(byte[] formattedkey) throws IOException {
		if (!isFormatted(formattedkey)) {
			throw new IOException("Key is not formatted in the PuTTY key format!");
		}
		return new PuTTYPrivateKeyFile(formattedkey);
	}

}
