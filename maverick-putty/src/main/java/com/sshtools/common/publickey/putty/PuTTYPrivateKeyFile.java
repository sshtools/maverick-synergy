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
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
import com.sshtools.common.ssh.components.SshDsaPrivateKey;
import com.sshtools.common.ssh.components.SshDsaPublicKey;
import com.sshtools.common.ssh.components.SshHmac;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPrivateCrtKey;
import com.sshtools.common.ssh.components.jce.ECUtils;
import com.sshtools.common.ssh.components.jce.HmacSha1;
import com.sshtools.common.ssh.components.jce.HmacSha256;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPrivateKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPublicKey;
import com.sshtools.common.ssh.components.jce.SshEd25519PrivateKey;
import com.sshtools.common.ssh.components.jce.SshEd25519PrivateKeyJCE;
import com.sshtools.common.ssh.components.jce.SshEd25519PublicKeyJCE;
import com.sshtools.common.ssh.components.jce.SshEd448PrivateKeyJCE;
import com.sshtools.common.ssh.components.jce.SshEd448PublicKeyJCE;
import com.sshtools.common.util.Base64;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.Utils;

class PuTTYPrivateKeyFile implements SshPrivateKeyFile {

	private static final String V3_ENCRYPTION_CIPHER = "aes256-cbc";
	private static final String V3_KEY_DERIVATION = "Argon2id";
	private static final int V3_ARGON2_MEMORY_KB = 8192;
	private static final int V3_ARGON2_PASSES = 13;
	private static final int V3_ARGON2_PARALLELISM = 1;
	private static final int V3_ARGON2_SALT_BYTES = 16;

	byte[] formattedKey;
	String comments = "";
	
	PuTTYPrivateKeyFile(byte[] formattedKey) throws IOException {
		this.formattedKey = formattedKey;
	}

	/**
	 * Create a PuTTY v3 (unencrypted) private key file from the given key pair.
	 *
	 * @param keyPair the SSH key pair to encode
	 * @param comment an optional comment string (may be null)
	 * @throws IOException  if the key type is unsupported or encoding fails
	 * @throws SshException if the public key cannot be encoded
	 */
	PuTTYPrivateKeyFile(SshKeyPair keyPair, String comment) throws IOException, SshException {
		this.comments = (comment == null ? "" : comment);
		this.formattedKey = formatV3Unencrypted(keyPair, this.comments);
	}

	private static byte[] formatV3Unencrypted(SshKeyPair keyPair, String comment) throws IOException, SshException {
		SshPublicKey pub = keyPair.getPublicKey();
		String algorithm = pub.getAlgorithm();

		byte[] publicBlob = pub.getEncoded();
		byte[] privateBlob = encodePuTTYPrivateBlob(keyPair);

		// MAC key for v3 unencrypted: SHA-256("putty-private-key-file-mac-key")
		byte[] macKey = DigestUtils.sha256("putty-private-key-file-mac-key".getBytes("UTF-8"));

		ByteArrayWriter macData = new ByteArrayWriter();
		macData.writeString(algorithm);
		macData.writeString("none");
		macData.writeString(comment);
		macData.writeBinaryString(publicBlob);
		macData.writeBinaryString(privateBlob);

		String macHex;
		try {
			HmacSha256 hmac = new HmacSha256();
			hmac.init(macKey);
			hmac.update(macData.toByteArray());
			macHex = Utils.bytesToHex(hmac.doFinal());
		} catch (SshException e) {
			throw new SshIOException(e);
		}

		// Base64-encode blobs wrapped at 64 chars per line (PuTTY standard)
		java.util.Base64.Encoder encoder = java.util.Base64.getMimeEncoder(64, new byte[]{'\n'});
		String publicBase64  = encoder.encodeToString(publicBlob);
		String privateBase64 = encoder.encodeToString(privateBlob);

		int publicLines  = countBase64Lines(publicBase64);
		int privateLines = countBase64Lines(privateBase64);

		StringBuilder sb = new StringBuilder();
		sb.append("PuTTY-User-Key-File-3: ").append(algorithm).append("\n");
		sb.append("Encryption: none\n");
		sb.append("Comment: ").append(comment).append("\n");
		sb.append("Public-Lines: ").append(publicLines).append("\n");
		sb.append(publicBase64).append("\n");
		sb.append("Private-Lines: ").append(privateLines).append("\n");
		sb.append(privateBase64).append("\n");
		sb.append("Private-MAC: ").append(macHex).append("\n");

		return sb.toString().getBytes("UTF-8");
	}

	/**
	 * Count the number of base64 lines in a string produced by
	 * {@link java.util.Base64.Encoder#encodeToString} (which does not add a
	 * trailing line-separator).
	 */
	private static int countBase64Lines(String base64) {
		if (base64.isEmpty()) return 0;
		return (int) base64.chars().filter(c -> c == '\n').count() + 1;
	}

	private static byte[] formatV3Encrypted(SshKeyPair keyPair, String comment, String passphrase)
			throws IOException, SshException {
		SshPublicKey pub = keyPair.getPublicKey();
		String algorithm = pub.getAlgorithm();

		byte[] publicBlob = pub.getEncoded();
		byte[] privateBlob = encodePuTTYPrivateBlob(keyPair);

		privateBlob = padToBlockSize(privateBlob, 16);

		byte[] salt = new byte[V3_ARGON2_SALT_BYTES];
		new SecureRandom().nextBytes(salt);

		byte[] keydata = generate(Argon2Parameters.ARGON2_id, V3_ARGON2_PASSES, V3_ARGON2_MEMORY_KB,
				V3_ARGON2_PARALLELISM, passphrase.getBytes("UTF-8"), salt, 80);

		byte[] key = new byte[32];
		byte[] iv = new byte[16];
		byte[] macKey = new byte[32];
		System.arraycopy(keydata, 0, key, 0, key.length);
		System.arraycopy(keydata, key.length, iv, 0, iv.length);
		System.arraycopy(keydata, key.length + iv.length, macKey, 0, macKey.length);

		ByteArrayWriter macData = new ByteArrayWriter();
		macData.writeString(algorithm);
		macData.writeString(V3_ENCRYPTION_CIPHER);
		macData.writeString(comment);
		macData.writeBinaryString(publicBlob);
		macData.writeBinaryString(privateBlob);

		String macHex;
		try {
			HmacSha256 hmac = new HmacSha256();
			hmac.init(macKey);
			hmac.update(macData.toByteArray());
			macHex = Utils.bytesToHex(hmac.doFinal()).toLowerCase();
		} catch (SshException e) {
			throw new SshIOException(e);
		}

		byte[] encryptedPrivateBlob = encryptAes256Cbc(privateBlob, key, iv);

		java.util.Base64.Encoder encoder = java.util.Base64.getMimeEncoder(64, new byte[] { '\n' });
		String publicBase64 = encoder.encodeToString(publicBlob);
		String privateBase64 = encoder.encodeToString(encryptedPrivateBlob);

		int publicLines = countBase64Lines(publicBase64);
		int privateLines = countBase64Lines(privateBase64);

		StringBuilder sb = new StringBuilder();
		sb.append("PuTTY-User-Key-File-3: ").append(algorithm).append("\n");
		sb.append("Encryption: ").append(V3_ENCRYPTION_CIPHER).append("\n");
		sb.append("Comment: ").append(comment).append("\n");
		sb.append("Public-Lines: ").append(publicLines).append("\n");
		sb.append(publicBase64).append("\n");
		sb.append("Private-Lines: ").append(privateLines).append("\n");
		sb.append(privateBase64).append("\n");
		sb.append("Key-Derivation: ").append(V3_KEY_DERIVATION).append("\n");
		sb.append("Argon2-Memory: ").append(V3_ARGON2_MEMORY_KB).append("\n");
		sb.append("Argon2-Passes: ").append(V3_ARGON2_PASSES).append("\n");
		sb.append("Argon2-Parallelism: ").append(V3_ARGON2_PARALLELISM).append("\n");
		sb.append("Argon2-Salt: ").append(Utils.bytesToHex(salt).toLowerCase()).append("\n");
		sb.append("Private-MAC: ").append(macHex).append("\n");

		return sb.toString().getBytes("UTF-8");
	}

	private static byte[] padToBlockSize(byte[] data, int blockSize) {
		int remainder = data.length % blockSize;
		if (remainder == 0) {
			return data;
		}
		int paddedLength = data.length + (blockSize - remainder);
		byte[] padded = new byte[paddedLength];
		System.arraycopy(data, 0, padded, 0, data.length);
		return padded;
	}

	private boolean isV3Format() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(formattedKey)))) {
			String line = reader.readLine();
			return line != null && line.startsWith("PuTTY-User-Key-File-3:");
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Encode the private-key blob in PuTTY binary format (algorithm-specific).
	 */
	private static byte[] encodePuTTYPrivateBlob(SshKeyPair keyPair) throws IOException {
		var priv = keyPair.getPrivateKey();
		String algorithm = keyPair.getPublicKey().getAlgorithm();

		ByteArrayWriter w = new ByteArrayWriter();

		if (algorithm.equals("ssh-ed25519")) {
			w.writeBinaryString(((SshEd25519PrivateKey) priv).getSeed());

		} else if (algorithm.equals("ssh-ed448")) {
			// SshEd448PrivateKeyJCE implements SshEd25519PrivateKey (which has getSeed())
			w.writeBinaryString(((SshEd448PrivateKeyJCE) priv).getSeed());

		} else if (algorithm.equals("ssh-dss")) {
			w.writeBigInteger(((SshDsaPrivateKey) priv).getX());

		} else if (algorithm.equals("ssh-rsa")) {
			BigInteger d, p, q, iqmp;
			if (priv instanceof SshRsaPrivateCrtKey) {
				SshRsaPrivateCrtKey crt = (SshRsaPrivateCrtKey) priv;
				d    = crt.getPrivateExponent();
				p    = crt.getPrimeP();
				q    = crt.getPrimeQ();
				iqmp = crt.getCrtCoefficient();
			} else if (priv.getJCEPrivateKey() instanceof RSAPrivateCrtKey) {
				RSAPrivateCrtKey crt = (RSAPrivateCrtKey) priv.getJCEPrivateKey();
				d    = crt.getPrivateExponent();
				p    = crt.getPrimeP();
				q    = crt.getPrimeQ();
				iqmp = crt.getCrtCoefficient();
			} else {
				throw new IOException(
						"RSA private key does not contain CRT parameters required for PuTTY v3 format");
			}
			w.writeBigInteger(d);
			w.writeBigInteger(p);
			w.writeBigInteger(q);
			w.writeBigInteger(iqmp);

		} else if (algorithm.startsWith("ecdsa-")) {
			ECPrivateKey jceKey = (ECPrivateKey) ((Ssh2EcdsaSha2NistPrivateKey) priv).getJCEPrivateKey();
			byte[] s = jceKey.getS().toByteArray();
			// Strip leading sign byte from BigInteger two's-complement representation
			if (s.length > 0 && s[0] == 0) {
				byte[] stripped = new byte[s.length - 1];
				System.arraycopy(s, 1, stripped, 0, stripped.length);
				s = stripped;
			}
			w.writeBinaryString(s);

		} else {
			throw new IOException("Unsupported key algorithm for PuTTY v3 format: " + algorithm);
		}

		return w.toByteArray();
	}

	public boolean supportsPassphraseChange() {
		return isV3Format();
	}

	public String getType() {
		return "PuTTY";
	}

	public boolean isPassphraseProtected() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(formattedKey)));

		try {
			String line = reader.readLine();

			if (line != null && (line.startsWith("PuTTY-User-Key-File-3:")
					|| line.startsWith("PuTTY-User-Key-File-2:")
					|| line.equals("PuTTY-User-Key-File-1:"))) {
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
									String keyDerivation = keyParameters.get("Key-Derivation");
									if (keyDerivation == null) {
										SshCipher cipher = (SshCipher) JCEComponentManager.getInstance()
												.supportedSsh2CiphersCS().getInstance(encryption);
										prv = performSHA1Decryption(keyParameters, passphrase, cipher, prv, pub);
									} else {
										prv = performDecryption(keyParameters, passphrase, prv, pub);
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

	private byte[] performDecryption(Map<String, String> keyParameters, String passphrase, byte[] prv, byte[] pub)
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

		String pass = passphrase == null ? "" : passphrase;
		byte[] keydata = generate(version, passes, memory, paralledlism, pass.getBytes("UTF-8"), salt, 80);

		byte[] key = new byte[32];
		byte[] iv = new byte[16];
		byte[] mac = new byte[32];

		System.arraycopy(keydata, 0, key, 0, key.length);
		System.arraycopy(keydata, key.length, iv, 0, iv.length);
		System.arraycopy(keydata, key.length + iv.length, mac, 0, mac.length);

		prv = decryptAes256Cbc(prv, key, iv);
		
		assertMac(new HmacSha256(), keyParameters, prv, pub, mac);
		
		return prv;
	}

	private static byte[] encryptAes256Cbc(byte[] data, byte[] key, byte[] iv) throws IOException {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new IOException("Failed to encrypt PuTTY v3 private key blob", e);
		}
	}

	private static byte[] decryptAes256Cbc(byte[] data, byte[] key, byte[] iv) throws IOException {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new IOException("Failed to decrypt PuTTY v3 private key blob", e);
		}
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

	public void changePassphrase(String oldpassphrase, String newpassprase)
			throws IOException, InvalidPassphraseException {
		if (!isV3Format()) {
			throw new IOException("Changing passphrase is only supported for PuTTY v3 key files");
		}

		SshKeyPair pair = toKeyPair(oldpassphrase);
		String comment = comments == null ? "" : comments;

		try {
			if (newpassprase == null || newpassprase.isEmpty()) {
				formattedKey = formatV3Unencrypted(pair, comment);
			} else {
				formattedKey = formatV3Encrypted(pair, comment, newpassprase);
			}
		} catch (SshException e) {
			throw new IOException("Failed to reformat PuTTY v3 key during passphrase change", e);
		}
	}

	public byte[] getFormattedKey() throws IOException {
		return formattedKey;
	}

	private static byte[] generate(int version, int iterations, int memory, int parallelism, byte[] password, byte[] salt,
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
