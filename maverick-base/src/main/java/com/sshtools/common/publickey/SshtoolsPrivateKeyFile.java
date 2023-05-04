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

package com.sshtools.common.publickey;

import java.io.IOException;
import java.math.BigInteger;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.SshDsaPrivateKey;
import com.sshtools.common.ssh.components.SshDsaPublicKey;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshRsaPrivateKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * @author Lee David Painter
 */
class SshtoolsPrivateKeyFile extends Base64EncodedFileFormat implements
		SshPrivateKeyFile {

	public static String BEGIN = "---- BEGIN SSHTOOLS ENCRYPTED PRIVATE KEY ----";
	public static String END = "---- END SSHTOOLS ENCRYPTED PRIVATE KEY ----";
	private int cookie = 0x52f37abe;

	byte[] keyblob;

	SshtoolsPrivateKeyFile(byte[] formattedkey) throws IOException {
		super(BEGIN, END);

		// Process the keyfile
		keyblob = getKeyBlob(formattedkey);
	}

	SshtoolsPrivateKeyFile(SshKeyPair pair, String passphrase, String comment)
			throws IOException {
		super(BEGIN, END);
		setHeaderValue("Comment", comment);
		ByteArrayWriter baw = new ByteArrayWriter();

		try {
			// Generate the keyblob
			if (pair.getPrivateKey() instanceof SshDsaPrivateKey) {

				SshDsaPrivateKey key = (SshDsaPrivateKey) pair.getPrivateKey();
				SshDsaPublicKey pub = key.getPublicKey();
				baw.writeString("ssh-dss");
				baw.writeBigInteger(pub.getP());
				baw.writeBigInteger(pub.getQ());
				baw.writeBigInteger(pub.getG());
				baw.writeBigInteger(key.getX());

				keyblob = encryptKey(baw.toByteArray(), passphrase);

			} else if (pair.getPrivateKey() instanceof SshRsaPrivateKey) {

				SshRsaPrivateKey pri = (SshRsaPrivateKey) pair.getPrivateKey();
				SshRsaPublicKey pub = (SshRsaPublicKey) pair.getPublicKey();

				baw.writeString("ssh-rsa");
				baw.writeBigInteger(pub.getPublicExponent());
				baw.writeBigInteger(pub.getModulus());
				baw.writeBigInteger(pri.getPrivateExponent());

				keyblob = encryptKey(baw.toByteArray(), passphrase);
			} else {
				throw new IOException("Unsupported private key type!");
			}

		} finally {
			baw.close();
		}

	}

	public String getType() {
		return "SSHTools";
	}

	public boolean supportsPassphraseChange() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.publickey.SshPrivateKeyFile#isPassphraseProtected()
	 */
	public boolean isPassphraseProtected() {

		ByteArrayReader bar = new ByteArrayReader(keyblob);
		try {

			String type = bar.readString();

			if (type.equals("none")) {
				return false;
			}

			if (type.equalsIgnoreCase("3des-cbc")) {
				return true;
			}
		} catch (IOException ioe) {
		} finally {
			bar.close();
		}

		return false;
	}

	private byte[] encryptKey(byte[] key, String passphrase) throws IOException {

		ByteArrayWriter baw = new ByteArrayWriter();
		ByteArrayWriter data = new ByteArrayWriter();

		try {

			String type = "none";

			if (passphrase != null) {
				if (!passphrase.trim().equals("")) {
					// Encrypt the data
					type = "3DES-CBC";

					// Decrypt the key
					byte[] keydata = makePassphraseKey(passphrase);
					byte[] iv = new byte[8];

					ComponentManager.getInstance().getRND().nextBytes(iv);

					SshCipher cipher = (SshCipher) ComponentManager
							.getInstance().supportedSsh2CiphersCS()
							.getInstance("3des-cbc");
					cipher.init(SshCipher.ENCRYPT_MODE, iv, keydata);

					baw.writeString(type);
					baw.write(iv);

					data.writeInt(cookie);
					data.writeBinaryString(key);

					if (data.size() % cipher.getBlockSize() != 0) {
						int length = cipher.getBlockSize()
								- (data.size() % cipher.getBlockSize());
						byte[] padding = new byte[length];
						for (int i = 0; i < length; i++) {
							padding[i] = (byte) length;
						}

						data.write(padding);
					}

					byte[] blob = data.toByteArray();
					cipher.transform(blob, 0, blob, 0, blob.length);

					// Encrypt and write
					baw.writeBinaryString(blob);

					return baw.toByteArray();
				}
			}

			// Write the type of encryption
			baw.writeString(type);

			// Write the key blob
			baw.writeBinaryString(key);

			// Now set the keyblob to our new encrpyted (or not) blob
			return baw.toByteArray();
		} catch (SshException e) {
			throw new SshIOException(e);
		} finally {
			baw.close();
			data.close();
		}
	}

	private byte[] decryptKey(String passphrase) throws IOException,
			InvalidPassphraseException {

		ByteArrayReader bar = new ByteArrayReader(keyblob);

		try {
			byte[] decryptedkey;

			String type = bar.readString();

			if (type.equalsIgnoreCase("3des-cbc")) {
				// Decrypt the key
				byte[] keydata = makePassphraseKey(passphrase);
				byte[] iv = new byte[8];

				if (type.equals("3DES-CBC")) {
					bar.read(iv);
				}

				decryptedkey = bar.readBinaryString();

				SshCipher cipher = (SshCipher) ComponentManager.getInstance()
						.supportedSsh2CiphersCS().getInstance("3des-cbc");
				cipher.init(SshCipher.DECRYPT_MODE, iv, keydata);

				cipher.transform(decryptedkey, 0, decryptedkey, 0,
						decryptedkey.length);
				ByteArrayReader data = new ByteArrayReader(decryptedkey);

				try {
					if (data.readInt() == cookie) {
						decryptedkey = data.readBinaryString();
						// Process the key into an SshPrivatekey implentation
					} else {
						throw new InvalidPassphraseException();
					}
				} finally {
					data.close();
				}
			} else {
				decryptedkey = bar.readBinaryString();
			}

			return decryptedkey;
		} catch (SshException e) {
			throw new SshIOException(e);
		} finally {
			bar.close();
		}
	}

	public byte[] getFormattedKey() throws IOException {
		return formatKey(keyblob);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sshtools.publickey.SshPrivateKeyFile#toPrivateKey(java.lang.String)
	 */
	public SshKeyPair toKeyPair(String passphrase) throws IOException,
			InvalidPassphraseException {

		ByteArrayReader bar = new ByteArrayReader(decryptKey(passphrase));

		try {
			// Process the actual key and return the key pair

			String algorithm = bar.readString();

			if (algorithm.equals("ssh-dss")) {
				BigInteger p = bar.readBigInteger();
				BigInteger q = bar.readBigInteger();
				BigInteger g = bar.readBigInteger();
				BigInteger x = bar.readBigInteger();

				SshDsaPrivateKey prv = ComponentManager.getInstance()
						.createDsaPrivateKey(p, q, g, x, g.modPow(x, p));
				SshKeyPair pair = new SshKeyPair();

				pair.setPublicKey(prv.getPublicKey());
				pair.setPrivateKey(ComponentManager.getInstance()
						.createDsaPrivateKey(p, q, g, x, g.modPow(x, p)));

				return pair;

			} else if (algorithm.equals("ssh-rsa")) {

				BigInteger publicExponent = bar.readBigInteger();
				BigInteger modulus = bar.readBigInteger();

				// Read the private key
				BigInteger privateExponent = bar.readBigInteger();

				SshKeyPair pair = new SshKeyPair();
				pair.setPublicKey(ComponentManager.getInstance()
						.createRsaPublicKey(modulus, publicExponent));
				pair.setPrivateKey(ComponentManager.getInstance()
						.createRsaPrivateKey(modulus, privateExponent));

				return pair;

			} else {
				throw new IOException("Unsupported private key algorithm type "
						+ algorithm);
			}
		} catch (SshException e) {
			throw new SshIOException(e);
		} finally {
			bar.close();
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
		keyblob = encryptKey(decryptKey(oldpassphrase), newpassphrase);

	}

	private byte[] makePassphraseKey(String passphrase) throws SshException {

		// Generate the key using the passphrase
		Digest md5 = (Digest) ComponentManager.getInstance().supportedDigests()
				.getInstance("MD5");
		md5.putBytes(passphrase.getBytes());

		byte[] key1 = md5.doFinal();

		md5.reset();
		md5.putBytes(passphrase.getBytes());
		md5.putBytes(key1);

		byte[] key2 = md5.doFinal();

		byte[] key = new byte[32];
		System.arraycopy(key1, 0, key, 0, 16);
		System.arraycopy(key2, 0, key, 16, 16);

		return key;

	}

	@Override
	public String getComment() {
		return "";
	}

}
