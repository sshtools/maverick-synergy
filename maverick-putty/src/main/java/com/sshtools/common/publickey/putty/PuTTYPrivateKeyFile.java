package com.sshtools.common.publickey.putty;

/*-
 * #%L
 * PuTTY Key Support
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.Arrays;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.DigestUtils;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.SshDsaPublicKey;
import com.sshtools.common.ssh.components.SshHmac;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.ECUtils;
import com.sshtools.common.ssh.components.jce.HmacSha1;
import com.sshtools.common.ssh.components.jce.HmacSha256;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPrivateKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPublicKey;
import com.sshtools.common.ssh.components.jce.SshEd25519PrivateKeyJCE;
import com.sshtools.common.ssh.components.jce.SshEd25519PublicKeyJCE;
import com.sshtools.common.ssh.components.jce.SshEd448PrivateKeyJCE;
import com.sshtools.common.ssh.components.jce.SshEd448PublicKeyJCE;
import com.sshtools.common.util.Base64;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.Utils;

class PuTTYPrivateKeyFile implements SshPrivateKeyFile {

	byte[] formattedKey;
	String comments = "";
	
	PuTTYPrivateKeyFile(byte[] formattedKey) throws IOException {
		this.formattedKey = formattedKey;
	}

	public boolean supportsPassphraseChange() {
		return false;
	}

	public String getType() {
		return "PuTTY";
	}

	public boolean isPassphraseProtected() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(formattedKey)));

		try {
			String line = reader.readLine();

			if (line != null && (line.startsWith("PuTTY-User-Key-File-2:") || line.equals("PuTTY-User-Key-File-1:"))) {
				line = reader.readLine();
				if (line != null && line.startsWith("Encryption:")) {
					String encryption = line.substring(line.indexOf(":") + 1).trim();
					if (encryption.equals("none")) {
						return false;
					}
					return true;
				}
			}
		} catch (Exception ex) {
		}

		return false;

	}

	public SshKeyPair toKeyPair(String passphrase) throws IOException, InvalidPassphraseException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(formattedKey)));

		boolean wasEncrpyted = false;

		try {
			String line = reader.readLine();

			if (line != null && (line.startsWith("PuTTY-User-Key-File-3:") || line.startsWith("PuTTY-User-Key-File-2:"))) {

				@SuppressWarnings("unused")
				int version = line.startsWith("PuTTY-User-Key-File-3:") ? 3 : 2;
				
				Map<String, String> keyParameters = new HashMap<>();
				String type = Utils.after(line, ':').trim();
				keyParameters.put("Algorithm", type);
				
				line = reader.readLine();

				if (line != null && line.startsWith("Encryption:")) {
					String encryption = line.substring(line.indexOf(":") + 1).trim();
					keyParameters.put(Utils.before(line, ':'), encryption);
					line = reader.readLine();

					if (line != null && line.startsWith("Comment:")) {
						keyParameters.put(Utils.before(line, ':'), comments = Utils.after(line, ':').trim());
						line = reader.readLine();

						if (line != null && line.startsWith("Public-Lines:")) {

							try {

								int publiclines = Integer.parseInt(line.substring(line.indexOf(":") + 1).trim());

								String publickey = "";
								for (int i = 0; i < publiclines; i++) {
									line = reader.readLine();
									if (line != null) {
										publickey += line;
									} else {
										throw new IOException("Corrupt public key data in PuTTY private key");
									}
								}

								byte[] pub = Base64.decode(publickey);


									
								String privatekey = "";

								while ((line = reader.readLine()) != null) {
									if (line.startsWith("Private-Lines:")) {
										int privatelines = Integer
												.parseInt(line.substring(line.indexOf(":") + 1).trim());

										for (int i = 0; i < privatelines; i++) {
											line = reader.readLine();
											if (line != null) {
												privatekey += line;
											} else {
												throw new IOException(
														"Corrupt private key data in PuTTY private key");
											}
										}
									} else {
										keyParameters.put(Utils.before(line, ':'), Utils.after(line, ':').trim());
									}
								}

								byte[] prv = Base64.decode(privatekey);

								if (!encryption.equals("none")) {
									SshCipher cipher = (SshCipher) JCEComponentManager.getInstance()
											.supportedSsh2CiphersCS().getInstance(encryption);

									String keyDerivation = keyParameters.get("Key-Derivation");
									if (keyDerivation == null) {
										prv = performSHA1Decryption(keyParameters, passphrase, cipher, prv, pub);
									} else {
										prv = performDecryption(keyParameters, passphrase, cipher, prv, pub);
									}

									wasEncrpyted = true;

								} 
								
								// Read the private key data
								ByteArrayReader bar = new ByteArrayReader(prv);

								try {

									
									// Convert the private key into the
									// format requried by J2SSH
									if (type.equals("ssh-dss")) {
										return readDsaKey(pub, bar);
									} else if (type.equals("ssh-rsa")) {
										return readRsaKey(pub, bar);
									} else if (type.equals("ssh-ed25519")) {
										return readEd25519Key(pub, bar);
									} else if (type.equals("ssh-ed448")) {
										return readEd448Key(pub, bar);
									} else if (type.startsWith("ecdsa")) {
										return readEcdsaKey(pub, bar);
									} else {
										throw new IOException("Unexpected key type " + type);
									}
								} finally {
									bar.close();
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
				throw new IOException("The PuTTY key could not be read! " + ex.getMessage());
		}

		if (wasEncrpyted)
			throw new InvalidPassphraseException();
		throw new IOException("The PuTTY key could not be read! Invalid format");

	}

	private byte[] performDecryption(Map<String, String> keyParameters, String passphrase, SshCipher cipher, byte[] prv, byte[] pub)
			throws IOException {

		String keyDerivation = keyParameters.get("Key-Derivation");
		int version;
		switch (keyDerivation) {
		case "Argon2d":
			version = Argon2Parameters.ARGON2_d;
			break;
		case "Argon2i":
			version = Argon2Parameters.ARGON2_i;
			break;
		case "Argon2id":
			version = Argon2Parameters.ARGON2_id;
			break;
		default:
			throw new IOException("Unexpected Key-Derivation value " + keyDerivation);
		}

		int memory = Integer.parseInt(keyParameters.get("Argon2-Memory"));
		int passes = Integer.parseInt(keyParameters.get("Argon2-Passes"));
		int paralledlism = Integer.parseInt(keyParameters.get("Argon2-Parallelism"));

		byte[] salt = Utils.hexToBytes(keyParameters.get("Argon2-Salt"));

		byte[] keydata = generate(version, passes, memory, paralledlism, passphrase.getBytes("UTF-8"), salt, 80);

		byte[] key = new byte[32];
		byte[] iv = new byte[16];
		byte[] mac = new byte[32];

		System.arraycopy(keydata, 0, key, 0, key.length);
		System.arraycopy(keydata, key.length, iv, 0, iv.length);
		System.arraycopy(keydata, key.length + iv.length, mac, 0, mac.length);

		cipher.init(SshCipher.DECRYPT_MODE, iv, key);

		cipher.transform(prv);
		
		assertMac(new HmacSha256(), keyParameters, prv, pub, mac);
		
		return prv;
	}

	private byte[] performSHA1Decryption(Map<String,String> keyParameters, String passphrase, SshCipher cipher, byte[] prv, byte[] pub)
			throws IOException, SshException {
		byte[] iv = new byte[40];
		byte[] key = new byte[40];

		Digest hash = (Digest) ComponentManager.getInstance().supportedDigests().getInstance("SHA-1");
		hash.putInt(0);
		hash.putBytes(passphrase.getBytes());
		byte[] key1 = hash.doFinal();

		hash.putInt(1);
		hash.putBytes(passphrase.getBytes());
		byte[] key2 = hash.doFinal();

		System.arraycopy(key1, 0, key, 0, 20);
		System.arraycopy(key2, 0, key, 20, 20);

		cipher.init(SshCipher.DECRYPT_MODE, iv, key);

		cipher.transform(prv);
		
		ByteArrayWriter init = new ByteArrayWriter();
		init.write("putty-private-key-file-mac-key".getBytes("UTF-8"));
		if(passphrase != null) {
			init.write(passphrase.getBytes("UTF-8"));
		}
		
		assertMac(new HmacSha1(), keyParameters, prv, pub, DigestUtils.sha1(init.toByteArray()));
		
		
		return prv;
	}

	private void assertMac(SshHmac digest, Map<String,String> keyParameters, byte[] prv, byte[] pub, byte[] key) throws IOException {
		ByteArrayWriter w = new ByteArrayWriter();
		w.writeString(keyParameters.get("Algorithm"));
		w.writeString(keyParameters.get("Encryption"));
		w.writeString(keyParameters.get("Comment"));
		w.writeBinaryString(pub);
		w.writeBinaryString(prv);
		
		try {
			
			digest.init(key);
			digest.update(w.toByteArray());
			
			byte[] m = digest.doFinal();
			byte[] m2 = Utils.hexToBytes(keyParameters.get("Private-MAC"));
			
			if(!Arrays.areEqual(m, m2)) {
				throw new IOException("Invalid mac in PuTTY private key file");
			}
		} catch (SshException e) {
			throw new SshIOException(e);
		}
	}
	private SshKeyPair readDsaKey(byte[] publickey, ByteArrayReader bar) throws SshException, IOException {
		
		try(ByteArrayReader pub = new ByteArrayReader(publickey)) {
		
			// Read the required variables
			// from
			// the public key
			pub.readString(); // Ignore sice
								// we
								// already
								// have
								// it
			BigInteger p = pub.readBigInteger();
			BigInteger q = pub.readBigInteger();
			BigInteger g = pub.readBigInteger();
			BigInteger y = pub.readBigInteger();
	
			/*
			 * And for "ssh-dss", it will be composed of
			 * 
			 * mpint x (the private key parameter) [ string hash 20-byte hash of mpints p ||
			 * q || g only in old format ]
			 */
	
			// now read the private exponent
			// from the private key
			BigInteger x = bar.readBigInteger();
	
			SshKeyPair pair = new SshKeyPair();
			SshDsaPublicKey publ = ComponentManager.getInstance().createDsaPublicKey(p, q, g, y);
			pair.setPublicKey(publ);
	
			pair.setPrivateKey(ComponentManager.getInstance().createDsaPrivateKey(p, q, g, x, publ.getY()));
	
			return pair;
		}
	}

	private SshKeyPair readRsaKey(byte[] publickey, ByteArrayReader bar) throws IOException, SshException {

		try(ByteArrayReader pub = new ByteArrayReader(publickey)) {
			pub.readString(); // Ignore
	
			BigInteger publicExponent = pub.readBigInteger();
			BigInteger modulus = pub.readBigInteger();
	
			/*
			 * mpint private_exponent mpint p (the larger of the two primes) mpint q (the
			 * smaller prime) mpint iqmp (the inverse of q modulo p) data padding (to reach
			 * a multiple of the cipher block size)
			 */
	
			// Read the private key
			// variables
			// from putty file
			BigInteger privateExponent = bar.readBigInteger();
	
			SshKeyPair pair = new SshKeyPair();
	
			pair.setPublicKey(ComponentManager.getInstance().createRsaPublicKey(modulus, publicExponent));
			pair.setPrivateKey(ComponentManager.getInstance().createRsaPrivateKey(modulus, privateExponent));
	
			return pair;
		}
	}

	private SshKeyPair readEcdsaKey(byte[] publickey, ByteArrayReader bar) throws IOException, SshException {

		SshKeyPair pair = new SshKeyPair();

		try {

			SshPublicKey p = SshPublicKeyFileFactory.decodeSSH2PublicKey(publickey);

			pair.setPublicKey(p);
			byte[] privateKey = bar.readBinaryString();
			ECPrivateKey prv = ECUtils.decodePrivateKey(privateKey,
					(ECPublicKey) ((Ssh2EcdsaSha2NistPublicKey) pair.getPublicKey()).getJCEPublicKey());
			pair.setPrivateKey(new Ssh2EcdsaSha2NistPrivateKey(prv, ((Ssh2EcdsaSha2NistPublicKey) p).getCurve()));
			return pair;

		} catch (InvalidKeySpecException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private SshKeyPair readEd25519Key(byte[] publickey, ByteArrayReader bar) throws IOException, SshException,
			NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {

		try(ByteArrayReader pub = new ByteArrayReader(publickey)) {
			SshKeyPair pair = new SshKeyPair();
	
			@SuppressWarnings("unused")
			String algorithm = pub.readString();
			byte[] publicKey = pub.readBinaryString();
			pair.setPublicKey(new SshEd25519PublicKeyJCE(publicKey));
	
			byte[] privateKey = bar.readBinaryString();
	
			pair.setPrivateKey(new SshEd25519PrivateKeyJCE(privateKey, publicKey));
	
			return pair;
		}
	}

	private SshKeyPair readEd448Key(byte[] publickey, ByteArrayReader bar) throws IOException, SshException,
			NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {

		try(ByteArrayReader pub = new ByteArrayReader(publickey)) {
			SshKeyPair pair = new SshKeyPair();
	
			@SuppressWarnings("unused")
			String algorithm = pub.readString();
			byte[] publicKey = pub.readBinaryString();
			pair.setPublicKey(new SshEd448PublicKeyJCE(publicKey));
	
			byte[] privateKey = bar.readBinaryString();
	
			pair.setPrivateKey(new SshEd448PrivateKeyJCE(privateKey));
	
			return pair;
		}
	}

	public void changePassphrase(String oldpassphrase, String newpassprase) throws IOException {
		throw new IOException("Changing passphrase is not supported by the PuTTY key format engine");
	}

	public byte[] getFormattedKey() throws IOException {
		return formattedKey;
	}

	private byte[] generate(int version, int iterations, int memory, int parallelism, byte[] password, byte[] salt,
			int outputLength) {
		Argon2Parameters.Builder builder = new Argon2Parameters.Builder(version)
				.withVersion(Argon2Parameters.ARGON2_VERSION_13)
				.withIterations(iterations)
				.withMemoryAsKB(memory)
				.withParallelism(parallelism)
				.withSalt(salt);

		Argon2BytesGenerator gen = new Argon2BytesGenerator();

		gen.init(builder.build());

		byte[] result = new byte[outputLength];

		gen.generateBytes(password, result, 0, result.length);
		return result;
	}

	@Override
	public String getComment() {
		return comments;
	}

}
