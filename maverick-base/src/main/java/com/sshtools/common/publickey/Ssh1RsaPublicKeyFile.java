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
import java.math.BigInteger;
import java.util.StringTokenizer;

import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;

/**
 * @author Lee David Painter
 */
class Ssh1RsaPublicKeyFile implements SshPublicKeyFile {

	String formattedkey;

	public Ssh1RsaPublicKeyFile(byte[] formattedkey) throws IOException {
		this.formattedkey = new String(formattedkey);
		toPublicKey(); // Validate
	}

	public Ssh1RsaPublicKeyFile(SshPublicKey key) throws IOException {

		if (key instanceof SshRsaPublicKey) {
			formattedkey = String.valueOf(((SshRsaPublicKey) key).getModulus()
					.bitLength())
					+ " "
					+ ((SshRsaPublicKey) key).getPublicExponent()
					+ " "
					+ ((SshRsaPublicKey) key).getModulus();
			toPublicKey(); // Validate
		} else {
			throw new IOException("SSH1 public keys must be rsa");
		}
	}

	public byte[] getFormattedKey() {
		return formattedkey.getBytes();
	}

	public SshPublicKey toPublicKey() throws IOException {
		StringTokenizer tokens = new StringTokenizer(formattedkey.trim(), " ");

		try {
			// int bitlength =
			Integer.parseInt((String) tokens.nextElement());
			String e = (String) tokens.nextElement();
			String n = (String) tokens.nextElement();

			BigInteger publicExponent = new BigInteger(e);
			BigInteger modulus = new BigInteger(n);

			return ComponentManager.getInstance().createRsaPublicKey(modulus,
					publicExponent);
		} catch (Throwable ex) {
			throw new IOException("Invalid SSH1 public key format");
		}

	}

	public String getComment() {
		return "";
	}

	public String getOptions() {
		return null;
	}

	public static boolean isFormatted(byte[] formattedkey) {

		try {
			StringTokenizer tokens = new StringTokenizer(new String(formattedkey, "UTF-8"), " ");
			
			Integer.parseInt((String) tokens.nextElement());
			tokens.nextElement();
			tokens.nextElement();

			return true;
		} catch (Throwable ex) {
			return false;
		}
	}

}
