/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
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
