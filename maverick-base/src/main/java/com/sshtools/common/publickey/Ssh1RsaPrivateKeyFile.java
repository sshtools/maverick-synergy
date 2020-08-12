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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
/* HEADER */
package com.sshtools.common.publickey;

import java.io.IOException;
import java.math.BigInteger;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshRsaPrivateCrtKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.ssh.components.SshSecureRandomGenerator;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * @author Lee David Painter
 */
class Ssh1RsaPrivateKeyFile implements SshPrivateKeyFile {
	public final static String IDENTIFIER = "SSH PRIVATE KEY FILE FORMAT 1.1\n";
	String comment;
	byte[] formattedkey;

	Ssh1RsaPrivateKeyFile(byte[] formattedkey) throws IOException {
		if (isFormatted(formattedkey)) {
			this.formattedkey = formattedkey;
		} else {
			throw new IOException("SSH1 RSA Key required");
		}
	}

	Ssh1RsaPrivateKeyFile(SshKeyPair pair, String passphrase, String comment)
			throws IOException {
		formattedkey = encryptKey(pair, passphrase, comment);
	}

	public boolean supportsPassphraseChange() {
		return true;
	}

	public String getType() {
		return "SSH1";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.publickey.SshPrivateKeyFile#isPassphraseProtected()
	 */
	public boolean isPassphraseProtected() {

		ByteArrayReader bar = new ByteArrayReader(formattedkey);
		try {

			byte[] id = new byte[IDENTIFIER.length()];
			bar.read(id);
			String idStr = new String(id);
			bar.read();
			if (!idStr.equals(IDENTIFIER)) {
				return false;
			}
			int cipherType = bar.read();
			return cipherType != 0;
		} catch (IOException ex) {
			return false;
		} finally {
			bar.close();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.publickey.SshPrivateKeyFile#toKeyPair(java.lang.String)
	 */
	public SshKeyPair toKeyPair(String passphrase) throws IOException,
			InvalidPassphraseException {
		return parse(formattedkey, passphrase);
	}

	public static boolean isFormatted(byte[] formattedkey) {
		String tmp = new String(formattedkey);
		return tmp.startsWith(IDENTIFIER.trim());
	}

	public SshKeyPair parse(byte[] formattedkey, String passphrase)
			throws IOException, InvalidPassphraseException {

		ByteArrayReader bar = new ByteArrayReader(formattedkey);

		try {

			byte[] id = new byte[IDENTIFIER.length()];
			bar.read(id);
			String idStr = new String(id);
			bar.read();

			if (!idStr.equals(IDENTIFIER)) {
				throw new IOException("RSA key file corrupt");
			}

			int cipherType = bar.read();
			if (cipherType != 3 && cipherType != 0) {
				throw new IOException(
						"Private key cipher type is not supported!");
			}

			bar.readInt(); // Skip a reserved int
			bar.readInt(); // Skip bits... (!?)

			BigInteger n = bar.readMPINT();
			BigInteger e = bar.readMPINT();
			SshRsaPublicKey publickey = ComponentManager.getInstance()
					.createRsaPublicKey(n, e);

			comment = bar.readString();

			byte[] rest = new byte[8192];
			int len = bar.read(rest);

			byte[] encrypted = new byte[len];
			System.arraycopy(rest, 0, encrypted, 0, len);

			if (cipherType == 3) {
				SshCipher cipher = (SshCipher) ComponentManager.getInstance()
						.supportedSsh1CiphersCS().getInstance("3");
				byte[] iv = new byte[cipher.getBlockSize()];
				cipher.init(SshCipher.DECRYPT_MODE, iv,
						makePassphraseKey(passphrase));

				cipher.transform(encrypted, 0, encrypted, 0, encrypted.length);
			}

			bar.close();
			bar = new ByteArrayReader(encrypted);

			try {
				byte c1 = (byte) bar.read();
				byte c2 = (byte) bar.read();
				byte c11 = (byte) bar.read();
				byte c22 = (byte) bar.read();

				if (c1 != c11 || c2 != c22) {
					throw new InvalidPassphraseException();
				}

				BigInteger d = bar.readMPINT();
				BigInteger u = bar.readMPINT();
				BigInteger p = bar.readMPINT();
				BigInteger q = bar.readMPINT();

				SshKeyPair pair = new SshKeyPair();
				pair.setPrivateKey(ComponentManager.getInstance()
						.createRsaPrivateCrtKey(publickey.getModulus(),
								publickey.getPublicExponent(), d, p, q, u));
				pair.setPublicKey(publickey);

				return pair;
			} finally {
				bar.close();
			}
		} catch (SshException e) {
			throw new SshIOException(e);
		} finally {
			bar.close();
		}
	}

	public byte[] encryptKey(SshKeyPair pair, String passphrase, String comment)
			throws IOException {
		
		ByteArrayWriter baw = new ByteArrayWriter();
		
		try {
			if (pair.getPrivateKey() instanceof SshRsaPrivateCrtKey) {

				SshRsaPrivateCrtKey privatekey = (SshRsaPrivateCrtKey) pair
						.getPrivateKey();
				

				byte[] c = new byte[2];
				SshSecureRandomGenerator rnd = ComponentManager.getInstance()
						.getRND();
				rnd.nextBytes(c);
				baw.write(c[0]);
				baw.write(c[1]);
				baw.write(c[0]);
				baw.write(c[1]);
				baw.writeMPINT(privatekey.getPrivateExponent());
				baw.writeMPINT(privatekey.getCrtCoefficient());
				baw.writeMPINT(privatekey.getPrimeP());
				baw.writeMPINT(privatekey.getPrimeQ());

				byte[] encrypted = baw.toByteArray();
				c = new byte[(8 - (encrypted.length % 8)) + encrypted.length];
				System.arraycopy(encrypted, 0, c, 0, encrypted.length);
				encrypted = c;

				int cipherType = 3;

				SshCipher cipher = (SshCipher) ComponentManager.getInstance()
						.supportedSsh1CiphersCS().getInstance("3");
				byte[] iv = new byte[cipher.getBlockSize()];
				cipher.init(SshCipher.ENCRYPT_MODE, iv,
						makePassphraseKey(passphrase));

				cipher.transform(encrypted, 0, encrypted, 0, encrypted.length);

				baw.reset();

				baw.write(IDENTIFIER.getBytes());
				baw.write(0);

				baw.write(cipherType);
				baw.writeInt(0);
				baw.writeInt(0);
				baw.writeMPINT(privatekey.getModulus());
				baw.writeMPINT(privatekey.getPublicExponent());
				baw.writeString(comment);

				baw.write(encrypted, 0, encrypted.length);

				return baw.toByteArray();
			}
			throw new IOException("RSA Private key required!");
		} catch (SshException e) {
			throw new SshIOException(e);
		} finally {
			baw.close();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sshtools.publickey.SshPrivateKeyFile#changePassphrase(java.lang.String
	 * , java.lang.String)
	 */
	public void changePassphrase(String oldpassphrase, String newpassphrase)
			throws IOException, InvalidPassphraseException {
		// TODO Auto-generated method stub
		formattedkey = encryptKey(parse(formattedkey, oldpassphrase),
				newpassphrase, comment);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.publickey.SshPrivateKeyFile#getFormattedKey()
	 */
	public byte[] getFormattedKey() {
		return formattedkey;
	}

	private byte[] makePassphraseKey(String passphrase) throws SshException {
		Digest hash = (Digest) ComponentManager.getInstance()
				.supportedDigests().getInstance("MD5");
		byte[] key = new byte[32];
		hash.putBytes(passphrase.getBytes());
		byte[] digest = hash.doFinal();
		System.arraycopy(digest, 0, key, 0, 16);
		System.arraycopy(digest, 0, key, 16, 16);
		return key;
	}

	@Override
	public String getComment() {
		return comment;
	}
}
