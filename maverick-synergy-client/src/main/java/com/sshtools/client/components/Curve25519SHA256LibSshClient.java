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

package com.sshtools.client.components;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.client.SshKeyExchangeClientFactory;

public class Curve25519SHA256LibSshClient extends Curve25519SHA256Client {
	
	public static class Curve25519SHA256LibSshClientFactory implements SshKeyExchangeClientFactory<Curve25519SHA256LibSshClient> {
		@Override
		public Curve25519SHA256LibSshClient create() throws NoSuchAlgorithmException, IOException {
			return new Curve25519SHA256LibSshClient();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CURVE25519_SHA2_AT_LIBSSH_ORG };
		}
	}

	public static final String CURVE25519_SHA2_AT_LIBSSH_ORG = "curve25519-sha256@libssh.org";

	public Curve25519SHA256LibSshClient() {
		super(CURVE25519_SHA2_AT_LIBSSH_ORG);
	}
}
