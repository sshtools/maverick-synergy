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

package com.sshtools.common.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.sshtools.common.knownhosts.KnownHostsKeyVerification;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class KnownHostsTests extends AbstractKnownHostsTests {

	@Override
	public KnownHostsKeyVerification loadKnownHosts(InputStream in) throws SshException, IOException {
		return new KnownHostsKeyVerification(in);
	}

	class LegacyKnownHostsKeyVerification extends KnownHostsKeyVerification {

		public LegacyKnownHostsKeyVerification(InputStream knownHosts) throws SshException, IOException {
			super(knownHosts);
		}

		@Override
		public void onHostKeyMismatch(String host, List<SshPublicKey> allowedHostKey, SshPublicKey actualHostKey)
				throws SshException {
			
		}

		@Override
		public void onUnknownHost(String host, SshPublicKey key) throws SshException {
			
		}
		
	}
}
