/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
/* HEADER */
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
