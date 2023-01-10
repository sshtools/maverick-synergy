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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SignatureGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * Implements public key authentication taking a separately loaded SshKeyPair as the private key for authentication.
 */
public class ExternalKeyAuthenticator extends PublicKeyAuthenticator {

	SignatureGenerator signatureGenerator;
	List<SshPublicKey> publickeys;
	SshPublicKey authenticatingKey = null;

	public ExternalKeyAuthenticator(SignatureGenerator signatureGenerator) throws IOException {
		this.signatureGenerator = signatureGenerator;
		this.publickeys = new ArrayList<>(signatureGenerator.getPublicKeys());
	}

	@Override
	protected SignatureGenerator getSignatureGenerator() throws IOException, InvalidPassphraseException {
		return signatureGenerator;
	}
	
	@Override
	protected SshPublicKey getNextKey() throws IOException {
		return authenticatingKey;
	}

	@Override
	protected SshKeyPair getAuthenticatingKey() throws IOException, InvalidPassphraseException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean hasCredentialsRemaining() {
		if(!publickeys.isEmpty()) {
			authenticatingKey = publickeys.remove(0);
			if(Log.isDebugEnabled()) {
				Log.debug("Using key {} {}", authenticatingKey.getAlgorithm(), SshKeyUtils.getFingerprint(authenticatingKey));
			}
			return true;
		}
		return false;
	}


	

}
