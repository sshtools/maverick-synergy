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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.SshDsaPublicKey;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.AES256Cbc;
import com.sshtools.common.util.Base64;
import com.sshtools.common.util.ByteArrayReader;

class PuTTYPrivateKeyFile implements SshPrivateKeyFile {

	byte[] formattedKey;

	PuTTYPrivateKeyFile(byte[] formattedKey) throws IOException {

		if (!isFormatted(formattedKey)) {
			throw new IOException(
					"Key is not formatted in the PuTTY key format!");
		}

		this.formattedKey = formattedKey;
	}

	public boolean supportsPassphraseChange() {
		return false;
	}

	public String getType() {
		return "PuTTY";
	}

	public boolean isPassphraseProtected() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(formattedKey)));

		try {
			String line = reader.readLine();

			if (line != null
					&& (line.startsWith("PuTTY-User-Key-File-2:") || line
							.equals("PuTTY-User-Key-File-1:"))) {
				line = reader.readLine();
				if (line != null && line.startsWith("Encryption:")) {
					String encryption = line.substring(line.indexOf(":") + 1)
							.trim();
					if (encryption.equals("aes256-cbc"))
						return true;
				}
			}
		} catch (Exception ex) {
		}

		return false;

	}

	public static boolean isFormatted(byte[] formattedKey) {

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(formattedKey)));

		try {
			String line = reader.readLine();

			return (line != null && (line.startsWith("PuTTY-User-Key-File-2:") || line
					.equals("PuTTY-User-Key-File-1:")));
		} catch (IOException ex) {
			return false;
		}

	}

	public SshKeyPair toKeyPair(String passphrase) throws IOException,
			InvalidPassphraseException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(formattedKey)));

		boolean wasEncrpyted = false;

		try {
			String line = reader.readLine();

			if (line != null
					&& (line.startsWith("PuTTY-User-Key-File-2:") || line
							.equals("PuTTY-User-Key-File-1:"))) {

				int format = line.startsWith("PuTTY-User-Key-File-2:") ? 2 : 1;
				String type = line.substring(line.indexOf(":") + 1).trim();

				line = reader.readLine();

				if (line != null && line.startsWith("Encryption:")) {
					String encryption = line.substring(line.indexOf(":") + 1)
							.trim();

					line = reader.readLine();

					if (line != null && line.startsWith("Comment:")) {
						// String comment = line.substring(line.indexOf(":") +
						// 1).trim();

						line = reader.readLine();

						if (line != null && line.startsWith("Public-Lines:")) {

							try {

								int publiclines = Integer.parseInt(line
										.substring(line.indexOf(":") + 1)
										.trim());

								String publickey = "";
								for (int i = 0; i < publiclines; i++) {
									line = reader.readLine();
									if (line != null) {
										publickey += line;
									} else {
										throw new IOException(
												"Corrupt public key data in PuTTY private key");
									}
								}

								ByteArrayReader pub = new ByteArrayReader(
										Base64.decode(publickey));

								try {
									line = reader.readLine();

									if (line != null
											&& line.startsWith("Private-Lines:")) {
										int privatelines = Integer
												.parseInt(line.substring(
														line.indexOf(":") + 1)
														.trim());

										String privatekey = "";
										for (int i = 0; i < privatelines; i++) {
											line = reader.readLine();
											if (line != null) {
												privatekey += line;
											} else {
												throw new IOException(
														"Corrupt private key data in PuTTY private key");
											}
										}

										byte[] blob = Base64.decode(privatekey);

										if (encryption.equals("aes256-cbc")) {
											SshCipher cipher = new AES256Cbc();

											byte[] iv = new byte[40];
											byte[] key = new byte[40];

											Digest hash = (Digest) ComponentManager
													.getInstance()
													.supportedDigests()
													.getInstance("SHA-1");
											hash.putInt(0);
											hash.putBytes(passphrase.getBytes());
											byte[] key1 = hash.doFinal();

											hash.putInt(1);
											hash.putBytes(passphrase.getBytes());
											byte[] key2 = hash.doFinal();

											System.arraycopy(key1, 0, key, 0,
													20);
											System.arraycopy(key2, 0, key, 20,
													20);

											cipher.init(SshCipher.DECRYPT_MODE,
													iv, key);

											cipher.transform(blob);

											wasEncrpyted = true;
										}

										// Read the private key data
										ByteArrayReader bar = new ByteArrayReader(
												blob);

										try {

											// Convert the private key into the
											// format requried by J2SSH
											if (type.equals("ssh-dss")) {

												// Read the required variables
												// from
												// the public key
												pub.readString(); // Ignore sice
																	// we
																	// already
																	// have
																	// it
												BigInteger p = pub
														.readBigInteger();
												BigInteger q = pub
														.readBigInteger();
												BigInteger g = pub
														.readBigInteger();
												BigInteger y = pub
														.readBigInteger();

												/*
												 * And for "ssh-dss", it will be
												 * composed of
												 * 
												 * mpint x (the private key
												 * parameter) [ string hash
												 * 20-byte hash of mpints p || q
												 * || g only in old format ]
												 */

												// now read the private exponent
												// from the private key
												BigInteger x = bar
														.readBigInteger();

												if (format == 1) {

												}

												SshKeyPair pair = new SshKeyPair();
												SshDsaPublicKey publ = ComponentManager
														.getInstance()
														.createDsaPublicKey(p,
																q, g, y);
												pair.setPublicKey(publ);

												pair.setPrivateKey(ComponentManager
														.getInstance()
														.createDsaPrivateKey(p,
																q, g, x,
																publ.getY()));

												return pair;
											} else if (type.equals("ssh-rsa")) {

												// Read the requried variables
												// from
												// the public key
												pub.readString(); // Ignore
																	// since we
																	// already
																	// have
																	// it
												BigInteger publicExponent = pub
														.readBigInteger();
												BigInteger modulus = pub
														.readBigInteger();

												/*
												 * mpint private_exponent mpint
												 * p (the larger of the two
												 * primes) mpint q (the smaller
												 * prime) mpint iqmp (the
												 * inverse of q modulo p) data
												 * padding (to reach a multiple
												 * of the cipher block size)
												 */

												// Read the private key
												// variables
												// from putty file
												BigInteger privateExponent = bar
														.readBigInteger();

												SshKeyPair pair = new SshKeyPair();

												pair.setPublicKey(ComponentManager
														.getInstance()
														.createRsaPublicKey(
																modulus,
																publicExponent));
												pair.setPrivateKey(ComponentManager
														.getInstance()
														.createRsaPrivateKey(
																modulus,
																privateExponent));

												return pair;
											} else {
												throw new IOException(
														"Unexpected key type "
																+ type);
											}
										} finally {
											bar.close();
										}
									}
								} finally {
									pub.close();
								}

							} catch (NumberFormatException ex) {
							} catch (OutOfMemoryError ex) {
							}

						}
					}
				}

			}
		} catch (Throwable ex) {
			if (!wasEncrpyted)
				throw new IOException("The PuTTY key could not be read! "
						+ ex.getMessage());
		}

		if (wasEncrpyted)
			throw new InvalidPassphraseException();
		throw new IOException("The PuTTY key could not be read! Invalid format");

	}

	public void changePassphrase(String oldpassphrase, String newpassprase)
			throws IOException {
		throw new IOException(
				"Changing passphrase is not supported by the PuTTY key format engine");
	}

	public byte[] getFormattedKey() throws IOException {
		return formattedKey;
	}

	@Override
	public String getComment() {
		return "";
	}

}
