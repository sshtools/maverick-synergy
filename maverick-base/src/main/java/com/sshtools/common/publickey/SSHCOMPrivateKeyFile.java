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

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.SshDsaPublicKey;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

class SSHCOMPrivateKeyFile extends Base64EncodedFileFormat implements
		SshPrivateKeyFile {

	static String BEGIN = "---- BEGIN SSH2 ENCRYPTED PRIVATE KEY ----";
	static String END = "---- END SSH2 ENCRYPTED PRIVATE KEY ----";

	byte[] formattedkey;

	SSHCOMPrivateKeyFile(byte[] formattedkey) throws IOException {
		super(BEGIN, END);

		if (!isFormatted(formattedkey)) {
			throw new IOException("Key is not formatted in the ssh.com format");
		}

		this.formattedkey = formattedkey;
	}

	public String getType() {
		return "SSH Communications Security";
	}

	public static boolean isFormatted(byte[] formattedkey) {
		return isFormatted(formattedkey, BEGIN, END);
	}

	public boolean supportsPassphraseChange() {
		return false;
	}

	public boolean isPassphraseProtected() {

		try {
			byte[] keyblob = getKeyBlob(formattedkey);
			ByteArrayReader bar = new ByteArrayReader(keyblob);
			try {
				long magic = bar.readInt();
				if (magic != 0x3f6ff9eb) {
					throw new IOException(
							"Invalid ssh.com key! Magic number not found");
				}
				// long size =
				bar.readInt();
				// String type =
				bar.readString();
				String cipher = bar.readString();

				return cipher.equals("3des-cbc");
			} finally {
				bar.close();
			}
		} catch (IOException ex) {
		}

		return false;

	}

	public SshKeyPair toKeyPair(String passphrase) throws IOException,
			InvalidPassphraseException {

		byte[] keyblob = getKeyBlob(formattedkey);
		boolean wasEncrypted = false;
		ByteArrayReader bar = new ByteArrayReader(keyblob);

		try {
			long magic = bar.readInt();

			if (magic != 0x3f6ff9eb)
				throw new IOException(
						"Invalid ssh.com key! Magic number not found");

			// long size =
			bar.readInt();
			String type = bar.readString();
			String cipher = bar.readString();
			byte[] blob = bar.readBinaryString();

			try {
				if (!cipher.equals("none")) {
					if (!cipher.equals("3des-cbc")) {
						throw new IOException("Unsupported cipher type "
								+ cipher + " in ssh.com private key");
					}

					SshCipher c = (SshCipher) ComponentManager.getInstance()
							.supportedSsh2CiphersCS().getInstance("3des-cbc");

					byte[] iv = new byte[32];
					byte[] keydata = makePassphraseKey(passphrase);

					c.init(SshCipher.DECRYPT_MODE, iv, keydata);

					c.transform(blob);
					wasEncrypted = true;

				}
			} catch (SshException e1) {
				throw new SshIOException(e1);
			}

			ByteArrayReader data = new ByteArrayReader(blob, 4,
					blob.length - 4);
			
			try {
				

				if (type.startsWith("if-modn{sign{rsa")) {

					BigInteger e = data.readMPINT32();
					BigInteger d = data.readMPINT32();
					BigInteger n = data.readMPINT32();
					@SuppressWarnings("unused")
					BigInteger u = data.readMPINT32();
					@SuppressWarnings("unused")
					BigInteger p = data.readMPINT32();
					@SuppressWarnings("unused")
					BigInteger q = data.readMPINT32();

					SshKeyPair pair = new SshKeyPair();

					pair.setPublicKey(ComponentManager.getInstance()
							.createRsaPublicKey(n, e));
					pair.setPrivateKey(ComponentManager.getInstance()
							.createRsaPrivateKey(n, d));

					return pair;

				} else if (type.startsWith("dl-modp{sign{dsa")) {

					long predefined = data.readInt();

					if (predefined != 0)
						throw new IOException(
								"Unexpected value in DSA key; this is an unsupported feature of ssh.com private keys");

					BigInteger p = data.readMPINT32();
					BigInteger g = data.readMPINT32();
					BigInteger q = data.readMPINT32();
					BigInteger y = data.readMPINT32();
					BigInteger x = data.readMPINT32();

					SshKeyPair pair = new SshKeyPair();

					SshDsaPublicKey pub = ComponentManager.getInstance()
							.createDsaPublicKey(p, q, g, y);
					pair.setPublicKey(pub);
					pair.setPrivateKey(ComponentManager.getInstance()
							.createDsaPrivateKey(p, q, g, x, pub.getY()));

					return pair;
				} else
					throw new IOException("Unsupported ssh.com key type "
							+ type);
			} catch (Throwable t) {
				if (wasEncrypted)
					throw new InvalidPassphraseException();
				throw new IOException("Bad SSH.com private key format!");
			} finally {
				data.close();
			}

		} finally {
			bar.close();
		}
	}

	private byte[] makePassphraseKey(String passphrase) throws IOException {
		ByteArrayWriter baw = new ByteArrayWriter();

		try {
			Digest hash = (Digest) ComponentManager.getInstance()
					.supportedDigests().getInstance("MD5");

			hash.putBytes(passphrase.getBytes());
			byte[] tmp = hash.doFinal();
			hash.reset();
			hash.putBytes(passphrase.getBytes());
			hash.putBytes(tmp);

			baw.write(tmp);
			baw.write(hash.doFinal());

			return baw.toByteArray();
		} catch (SshException e) {
			throw new SshIOException(e);
		} finally {
			baw.close();
		}
	}

	public void changePassphrase(String oldpassphrase, String newpassprase)
			throws IOException {
		/** @todo Implement this com.sshtools.publickey.SshPrivateKeyFile method */
		throw new IOException(
				"Changing passphrase is not supported by the ssh.com key format engine");
	}

	public byte[] getFormattedKey() throws IOException {
		return formattedkey;
	}

	@Override
	public String getComment() {
		return "";
	}

}
