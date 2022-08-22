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

package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshCipherFactory;

/**
 * An implementation of the Blowfish cipher using a JCE provider. If you have
 * enabled JCE usage there is no need to configure this separately.
 * 
 * @author Lee David Painter
 */
public class BlowfishCbc extends AbstractJCECipher {

	private static final String CIPHER = "blowfish-cbc";

	public static class BlowfishCbcFactory implements SshCipherFactory<BlowfishCbc> {

		@Override
		public BlowfishCbc create() throws NoSuchAlgorithmException, IOException {
			return new BlowfishCbc();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CIPHER };
		}

		@Override
		public boolean isEnabledByDefault() {
			return false;
		}
	}

	public BlowfishCbc() throws IOException {
		super(JCEAlgorithms.JCE_BLOWFISHCBCNOPADDING, "Blowfish", 16, CIPHER, SecurityLevel.WEAK, 0);
	}

}
