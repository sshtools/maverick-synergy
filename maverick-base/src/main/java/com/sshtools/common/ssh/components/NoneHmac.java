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

package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;

public class NoneHmac implements SshHmac {

	private static final String NONE = "none";

	public static class NoneHmacFactory implements SshHmacFactory<NoneHmac> {

		@Override
		public NoneHmac create() throws NoSuchAlgorithmException, IOException {
			return new NoneHmac();
		}

		@Override
		public String[] getKeys() {
			return new String[] { NONE };
		}
	}

	public int getMacSize() {
		return 0;
	}

	public int getMacLength() {
		return 0;
	}
	
	public void generate(long sequenceNo, byte[] data, int offset, int len,
			byte[] output, int start) {
	}

	public void init(byte[] keydata) throws SshException {
	}

	public boolean verify(long sequenceNo, byte[] data, int start, int len,
			byte[] mac, int offset) {
		return true;
	}

	public void update(byte[] b) {
	}

	public byte[] doFinal() {

		return new byte[0];
	}

	public String getAlgorithm() {
		return NONE;
	}

	public boolean isETM() {
		return false;
	}

	@Override
	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.WEAK;
	}

	@Override
	public int getPriority() {
		return 0;
	}
}
