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

package com.sshtools.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * Implements public key authentication, taking a com.sshtools.publickey.SshKeyPair object as the source private key.
 */
public class KeyPairAuthenticator extends PublicKeyAuthenticator {

	List<SshKeyPair> pairs;
	SshKeyPair authenticatingPair;
	
	public KeyPairAuthenticator(SshKeyPair pair) {
		this.pairs = new ArrayList<>(Arrays.asList(pair));
	}

	public KeyPairAuthenticator(SshKeyPair... identities) {
		this.pairs = new ArrayList<>(Arrays.asList(identities));
	}

	@Override
	protected SshPublicKey getPublicKey() {
		authenticatingPair = pairs.remove(0);
		return authenticatingPair.getPublicKey();
	}

	@Override
	protected SshKeyPair getAuthenticatingKey() {
		return authenticatingPair;
	}

	@Override
	protected boolean hasCredentialsRemaining() {
		return !pairs.isEmpty();
	}

}
