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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshSecureRandomGenerator;

/**
 * Secure random number generator implementation for JCE provider. 
 */
public class SecureRND implements SshSecureRandomGenerator {

	SecureRandom rnd;
	
	public SecureRND() throws NoSuchAlgorithmException {
		rnd = JCEProvider.getSecureRandom();
	}
	
	public void nextBytes(byte[] bytes) {
		rnd.nextBytes(bytes);
	}

	public void nextBytes(byte[] bytes, int off, int len) throws SshException {
		
		try {
			byte[] tmp = new byte[len];
			rnd.nextBytes(tmp);
			System.arraycopy(tmp, 0, bytes, off, len);
		} catch(ArrayIndexOutOfBoundsException ex){
			throw new SshException("ArrayIndexOutOfBoundsException: Index " + off + " on actual array length " + bytes.length + " with len=" + len, SshException.INTERNAL_ERROR);
		}
	}

	public int nextInt() {
		return rnd.nextInt();
	}

}
