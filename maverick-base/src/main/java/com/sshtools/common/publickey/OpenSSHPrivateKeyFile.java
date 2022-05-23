/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.SshDsaPrivateKey;
import com.sshtools.common.ssh.components.SshDsaPublicKey;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshRsaPrivateCrtKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.ssh.components.jce.AES128Cbc;
import com.sshtools.common.ssh.components.jce.AES192Cbc;
import com.sshtools.common.ssh.components.jce.AES256Cbc;
import com.sshtools.common.ssh.components.jce.ECUtils;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.Ssh2DsaPrivateKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPrivateKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPrivateCrtKey;
import com.sshtools.common.ssh.components.jce.SshEd25519PrivateKeyJCE;
import com.sshtools.common.ssh.components.jce.SshEd25519PublicKey;
import com.sshtools.common.ssh.components.jce.SshEd448PrivateKeyJCE;
import com.sshtools.common.ssh.components.jce.SshEd448PublicKey;
import com.sshtools.common.ssh.components.jce.TripleDesCbc;
import com.sshtools.common.util.BCryptKDF;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.SimpleASNReader;
import com.sshtools.common.util.SimpleASNWriter;
import com.sshtools.common.util.UnsignedInteger32;

public class OpenSSHPrivateKeyFile implements SshPrivateKeyFile {

	byte[] formattedkey;
	static final String AUTH_MAGIC = "openssh-key-v1";
	String comment = "";
	
	public OpenSSHPrivateKeyFile(byte[] formattedkey) throws IOException {
		if (!isFormatted(formattedkey)) {
			throw new IOException("Formatted key data is not a valid OpenSSH key format");
		}
		this.formattedkey = formattedkey;
	}

	public OpenSSHPrivateKeyFile(SshKeyPair pair, String passphrase, String comment) throws IOException {
		this.comment = comment;
		formattedkey = encryptKey(pair, passphrase);
	}

	public OpenSSHPrivateKeyFile() {
	}

	public String getComment() {
		return comment;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sshtools.publickey.SshPrivateKeyFile#isPassphraseProtected()
	 */
	public boolean isPassphraseProtected() {
		try {
			Reader r = new StringReader(new String(formattedkey, "US-ASCII"));
			PEMReader pem = new PEMReader(r);

			if (isPassphraseProtectedOpenSSHKeyFile()) {
				return true;
			} else {
				return pem.getHeader().containsKey("DEK-Info");
			}
		} catch (IOException e) {
			return true;
		}
	}

	private boolean isPassphraseProtectedOpenSSHKeyFile() throws IOException {

		Reader r = new StringReader(new String(formattedkey, "US-ASCII"));
		PEMReader pem = new PEMReader(r);

		if (pem.getType().equals(PEM.OPENSSH_PRIVATE_KEY)) {

			try (ByteArrayReader bar = new ByteArrayReader(pem.decryptPayload(null))) {

				byte[] auth_magic = new byte[14];
				bar.read(auth_magic);

				if (!Arrays.equals(auth_magic, AUTH_MAGIC.getBytes("UTF-8"))) {
					return false;
				}

				bar.skip(1); // NULL at end of AUTH_MAGIC

				String cipherName = bar.readString();
				return !cipherName.equals("none");
			}
		}
		return false;
	}

	public String getType() {
		return "OpenSSH";
	}

	public boolean supportsPassphraseChange() {
		return true;
	}

	public SshKeyPair toKeyPair(String passphrase) throws IOException, InvalidPassphraseException {

		Reader r = new StringReader(new String(formattedkey, "US-ASCII"));
		PEMReader pem = new PEMReader(r);
		byte[] payload = pem.decryptPayload(passphrase);

		SimpleASNReader asn = new SimpleASNReader(payload);

		try {
			if (PEM.DSA_PRIVATE_KEY.equals(pem.getType())) {
				return getDSAKeyPair(asn);
			} else if (PEM.RSA_PRIVATE_KEY.equals(pem.getType())) {
				return getRSAKeyPair(asn);
			} else if (PEM.EC_PRIVATE_KEY.equals(pem.getType())) {
				return getECKeyPair(asn);
			} else if (PEM.OPENSSH_PRIVATE_KEY.equals(pem.getType())) {
				return getOpenSSHKeyPair(payload, passphrase);
			} else {
				throw new IOException("Unsupported type: " + pem.getType());
			}
		} catch (SshException | IOException ex) {
			ex.printStackTrace();
			throw new InvalidPassphraseException(ex);
		}
	}

	private void writeOpenSSHKeyPair(ByteArrayWriter writer, SshKeyPair pair, String passphrase)
			throws IOException, NoSuchAlgorithmException, SshException {

		writer.write(AUTH_MAGIC.getBytes("UTF-8"));
		writer.write(0);

		String cipherName = "none";
		String kdfName = "none";
		byte[] salt = new byte[16];
		JCEComponentManager.getSecureRandom().nextBytes(salt);
		int rounds = 16;

		try (ByteArrayWriter options = new ByteArrayWriter()) {

			if (passphrase != null && passphrase.length() > 0) {
				cipherName = "aes256-ctr";
				kdfName = "bcrypt";
				options.writeBinaryString(salt);
				options.writeInt(rounds);
			}

			writer.writeString(cipherName);
			writer.writeString(kdfName);
			writer.writeBinaryString(options.toByteArray());

			writer.writeInt(1);
			writer.writeBinaryString(pair.getPublicKey().getEncoded());

			try (ByteArrayWriter privateKeyData = new ByteArrayWriter()) {
				int checksum = JCEComponentManager.getSecureRandom().nextInt() & 0x0FFFFFFF;

				privateKeyData.writeUINT32(new UnsignedInteger32(checksum));
				privateKeyData.writeUINT32(new UnsignedInteger32(checksum));
				privateKeyData.writeString(pair.getPrivateKey().getAlgorithm());

				String algorithm = pair.getPublicKey().getEncodingAlgorithm();
				switch (algorithm) {
				case "ssh-ed448":
				{
					byte[] a = ((SshEd448PublicKey) pair.getPublicKey()).getA();
					privateKeyData.writeBinaryString(a);
					byte[] sk = ((SshEd448PrivateKeyJCE) pair.getPrivateKey()).getSeed();
					privateKeyData.writeBinaryString(sk);
					break;
				}
				case "ssh-ed25519":
				{
					byte[] a = ((SshEd25519PublicKey) pair.getPublicKey()).getA();
					privateKeyData.writeBinaryString(a);
					byte[] sk = ((SshEd25519PrivateKeyJCE) pair.getPrivateKey()).getSeed();
					privateKeyData.writeInt(64);
					privateKeyData.write(sk);
					privateKeyData.write(a);

					break;
				}
				case "ssh-rsa": {
					SshRsaPublicKey publickey = (SshRsaPublicKey) pair.getPublicKey();
					SshRsaPrivateCrtKey privatekey = (SshRsaPrivateCrtKey) pair.getPrivateKey();
					privateKeyData.writeBigInteger(publickey.getModulus());
					privateKeyData.writeBigInteger(publickey.getPublicExponent());
					privateKeyData.writeBigInteger(privatekey.getPrivateExponent());
					privateKeyData.writeBigInteger(privatekey.getCrtCoefficient());
					privateKeyData.writeBigInteger(privatekey.getPrimeP());
					privateKeyData.writeBigInteger(privatekey.getPrimeQ());

					break;
				}
				case "ssh-dss": {
					SshDsaPublicKey publickey = (SshDsaPublicKey) pair.getPublicKey();
					privateKeyData.writeBigInteger(publickey.getP());
					privateKeyData.writeBigInteger(publickey.getQ());
					privateKeyData.writeBigInteger(publickey.getG());
					privateKeyData.writeBigInteger(publickey.getY());
					privateKeyData.writeBigInteger(((Ssh2DsaPrivateKey) pair.getPrivateKey()).getX());
					break;
				}
				case "ecdsa-sha2-nistp521":
				case "ecdsa-sha2-nistp384":
				case "ecdsa-sha2-nistp256": {
					privateKeyData.writeString(((Ssh2EcdsaSha2NistPublicKey) pair.getPublicKey()).getCurve());
					privateKeyData.writeBinaryString(((Ssh2EcdsaSha2NistPublicKey) pair.getPublicKey()).getPublicOctet());
					privateKeyData.writeBinaryString(((ECPrivateKey) pair.getPrivateKey().getJCEPrivateKey()).getS().toByteArray());
					break;
				}
				default:
					throw new IOException(
							String.format("Unsupported public key type %s for OpenSSH private key file format",
									pair.getPublicKey().getAlgorithm()));
				}

				if(comment==null) {
					privateKeyData.writeString(String.format("%s@%s", 
							System.getProperty("user.name"), InetAddress.getLocalHost().getHostName()));
				} else {
					privateKeyData.writeString(comment);
				}

				if (!cipherName.equals("none")) {

					SshCipher cipher = (SshCipher) ComponentManager.getInstance().supportedSsh2CiphersCS()
							.getInstance(cipherName);

					byte[] iv = new byte[cipher.getBlockSize()];
					byte[] key = new byte[cipher.getKeyLength()];

					byte[] keydata = BCryptKDF.bcrypt_pbkdf(passphrase.getBytes("UTF-8"), salt, iv.length + key.length,
							rounds);

					System.arraycopy(keydata, 0, key, 0, key.length);
					System.arraycopy(keydata, key.length, iv, 0, iv.length);

					cipher.init(SshCipher.ENCRYPT_MODE, iv, key);
					
					pad(privateKeyData, cipher.getBlockSize());
					byte[] payload = privateKeyData.toByteArray();
					cipher.transform(payload);
					writer.writeBinaryString(payload);
					
				} else {
					pad(privateKeyData, 8);
					writer.writeBinaryString(privateKeyData.toByteArray());
				}

			}
		}
	}

	private void pad(ByteArrayWriter privateKeyData, int blockSize) {
		
		int i = 0;
		while(privateKeyData.size() % blockSize != 0) {
			privateKeyData.write(++i);
		}
		
	}

	private SshKeyPair getOpenSSHKeyPair(byte[] payload, String passphrase)
			throws IOException, InvalidPassphraseException, SshException {

		ByteArrayReader bar = new ByteArrayReader(payload);

		try {
			byte[] auth_magic = new byte[14];
			bar.read(auth_magic);

			if (!Arrays.equals(auth_magic, AUTH_MAGIC.getBytes("UTF-8"))) {
				throw new IOException(String.format("Unexpected key format %s", new String(auth_magic)));
			}

			bar.skip(1); // NULL at end of AUTH_MAGIC

			String cipherName = bar.readString();
			String kdfName = bar.readString();
			try (ByteArrayReader optionsReader = new ByteArrayReader(bar.readBinaryString())) {

				SshCipher cipher = null;

				if (!kdfName.equals("none")) {
					if (!kdfName.equals("bcrypt")) {
						throw new IOException(String.format("Unsupported KDF type %s", kdfName));
					}
					
					if(passphrase==null) {
						throw new InvalidPassphraseException();
					}

					switch(cipherName) {
					case "aes128-cbc":
						cipher = new AES128Cbc();
						break;
					case "aes192-cbc":
						cipher = new AES192Cbc();
						break;
					case "aes256-cbc":
						cipher = new AES256Cbc();
						break;
					case "3des-cbc":
						cipher = new TripleDesCbc();
						break;
					default:
						break;
					}
					
					if(Objects.isNull(cipher)) {
						cipher = (SshCipher) 
							ComponentManager.getInstance().supportedSsh2CiphersCS()
								.getInstance(cipherName);
					}

					byte[] salt = optionsReader.readBinaryString();
					int rounds = (int) optionsReader.readInt();

					byte[] iv = new byte[cipher.getBlockSize()];
					byte[] key = new byte[cipher.getKeyLength()];

					byte[] keydata = BCryptKDF.bcrypt_pbkdf(passphrase.getBytes("UTF-8"), salt, iv.length + key.length,
							rounds);

					System.arraycopy(keydata, 0, key, 0, key.length);
					System.arraycopy(keydata, key.length, iv, 0, iv.length);

					cipher.init(SshCipher.DECRYPT_MODE, iv, key);

				}

				int count = (int) bar.readInt();

				SshKeyPair pair = new SshKeyPair();

				for (int i = 0; i < count; i++) {

					byte[] publicKey = bar.readBinaryString();
					pair.setPublicKey(SshPublicKeyFileFactory.decodeSSH2PublicKey(publicKey));
				}

				byte[] data = bar.readBinaryString();

				if (!cipherName.equals("none")) {
					cipher.transform(data);
				}

				ByteArrayReader privateReader = new ByteArrayReader(data);

				try {
					UnsignedInteger32 checkint = privateReader.readUINT32();
					UnsignedInteger32 checkint2 = privateReader.readUINT32();

					if (!checkint.equals(checkint2)) {
						throw new InvalidPassphraseException();
					}
					for (int i = 0; i < count; i++) {

						String algorithm = privateReader.readString();

						switch (algorithm) {
						case "ssh-ed448": {
							@SuppressWarnings("unused")
							byte[] publicKey = privateReader.readBinaryString();
							byte[] privateKey = privateReader.readBinaryString();
							pair.setPrivateKey(new SshEd448PrivateKeyJCE(privateKey));
							break;
						}
						case "ssh-ed25519": {
							byte[] publicKey = privateReader.readBinaryString();
							byte[] privateKey = privateReader.readBinaryString();
							pair.setPrivateKey(new SshEd25519PrivateKeyJCE(privateKey, publicKey));
							break;
						}
						case "ecdsa-sha2-nistp521":
						case "ecdsa-sha2-nistp384":
						case "ecdsa-sha2-nistp256": {
							try {
								String curve = privateReader.readString();
								@SuppressWarnings("unused")
								byte[] publicKey = privateReader.readBinaryString();
								byte[] privateKey = privateReader.readBinaryString();
								ECPrivateKey prv = ECUtils.decodePrivateKey(privateKey,
										(ECPublicKey) ((Ssh2EcdsaSha2NistPublicKey) pair.getPublicKey())
												.getJCEPublicKey());
								pair.setPrivateKey(new Ssh2EcdsaSha2NistPrivateKey(prv, curve));
							} catch (InvalidKeySpecException e) {
								throw new IOException(e.getMessage(), e);
							}
							break;
						}
						case "ssh-dss": {
							BigInteger p = privateReader.readBigInteger();
							BigInteger q = privateReader.readBigInteger();
							BigInteger g = privateReader.readBigInteger();
							BigInteger y = privateReader.readBigInteger();
							BigInteger x = privateReader.readBigInteger();

							pair.setPrivateKey(new Ssh2DsaPrivateKey(p, q, g, x, y));
							break;
						}
						case "ssh-rsa": {
							BigInteger n = privateReader.readBigInteger();
							BigInteger e = privateReader.readBigInteger();
							BigInteger d = privateReader.readBigInteger();
							BigInteger iqmp = privateReader.readBigInteger();
							BigInteger p = privateReader.readBigInteger();
							BigInteger q = privateReader.readBigInteger();

							/**
							 * Calculate additional CRT parameters
							 */
							BigInteger aux = q.subtract(BigInteger.ONE);
							BigInteger dmq1 = d.remainder(aux);
							aux = p.subtract(BigInteger.ONE);
							BigInteger dmp1 = d.remainder(aux);

							pair.setPrivateKey(new Ssh2RsaPrivateCrtKey(n, e, d, p, q, dmp1, dmq1, iqmp));
							break;
						}
						default:
							throw new IOException(
									String.format("Unsupported public key type '%s' in OpenSSH formatted private key",
											pair.getPublicKey().getAlgorithm()));
						}
						
						if(privateReader.available() >= 4) {
							this.comment = privateReader.readString();
						}
					}

					return pair;
				} finally {
					privateReader.close();
				}
			}

		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e.getMessage(), e);
		} catch(SshIOException e) { 
			throw e.getRealException();
		} catch (Throwable e) {
			if (e instanceof InvalidPassphraseException) {
				throw (InvalidPassphraseException) e;
			}
			throw new IOException(e);
		} finally {
			bar.close();
		}
	}

	public String oidByteArrayToString(byte[] o) {
		StringBuilder retVal = new StringBuilder();

		int[] oid = new int[o.length];
		for (int x = 0; x < o.length; x++) {
			oid[x] = o[x] & 0xFF;
		}

		for (int i = 0; i < oid.length; i++) {
			if (i == 0) {
				int b = oid[0] % 40;
				int a = (oid[0] - b) / 40;
				retVal.append(String.format("%d.%d", a, b));
			} else {
				if (oid[i] < 128)
					retVal.append(String.format(".%d", oid[i]));
				else {
					retVal.append(String.format(".%d", ((oid[i] - 128) * 128) + oid[i + 1]));
					i++;
				}
			}
		}

		return retVal.toString();
	}

	SshKeyPair getECKeyPair(SimpleASNReader asn) throws IOException {

		try {
			asn.assertByte(0x30); // SEQUENCE
			asn.getLength();

			asn.assertByte(0x02); // INTEGER (version)
			asn.getData();

			asn.assertByte(0x4);
			byte[] privateKey = asn.getData();

			asn.assertByte(0xA0);
			asn.getLength();
			asn.assertByte(0x6);
			byte[] namedCurve = asn.getData();

			asn.assertByte(0xA1);
			asn.getLength();
			asn.assertByte(0x03);
			byte[] publicKey = asn.getData();

			String curve = curveFromOOID(namedCurve);
			ECPublicKey pub = ECUtils.decodeKey(publicKey, curve);
			ECPrivateKey prv = ECUtils.decodePrivateKey(privateKey, pub);

			SshKeyPair pair = new SshKeyPair();
			pair.setPrivateKey(new Ssh2EcdsaSha2NistPrivateKey(prv, curve));
			pair.setPublicKey(new Ssh2EcdsaSha2NistPublicKey(pub, curve));

			return pair;
		} catch (Exception e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private String curveFromOOID(byte[] o) {
		String oid = oidByteArrayToString(o);
		if (oid.equals("1.2.840.10045.3.1.7")) {
			return "secp256r1";
		} else if (oid.equals("1.3.132.0.34")) {
			return "secp384r1";
		} else if (oid.equals("1.3.132.0.35")) {
			return "secp521r1";
		} else {
			throw new IllegalArgumentException("Unsupported OID " + oid);
		}
	}

	SshKeyPair getRSAKeyPair(SimpleASNReader asn) throws IOException {

		try {
			asn.assertByte(0x30); // SEQUENCE

			asn.getLength();
			asn.assertByte(0x02); // INTEGER (version)

			asn.getData();
			asn.assertByte(0x02); // INTEGER ()

			BigInteger modulus = new BigInteger(asn.getData());
			asn.assertByte(0x02); // INTEGER ()

			BigInteger publicExponent = new BigInteger(asn.getData());
			asn.assertByte(0x02); // INTEGER ()

			BigInteger privateExponent = new BigInteger(asn.getData());
			asn.assertByte(0x02); // INTEGER ()

			BigInteger primeP = new BigInteger(asn.getData());
			asn.assertByte(0x02); // INTEGER ()

			BigInteger primeQ = new BigInteger(asn.getData());
			asn.assertByte(0x02); // INTEGER ()

			BigInteger primeExponentP = new BigInteger(asn.getData());
			asn.assertByte(0x02); // INTEGER ()

			BigInteger primeExponentQ = new BigInteger(asn.getData());
			asn.assertByte(0x02); // INTEGER ()

			BigInteger crtCoefficient = new BigInteger(asn.getData());

			SshKeyPair pair = new SshKeyPair();
			pair.setPublicKey(ComponentManager.getInstance().createRsaPublicKey(modulus, publicExponent));
			pair.setPrivateKey(ComponentManager.getInstance().createRsaPrivateCrtKey(modulus, publicExponent,
					privateExponent, primeP, primeQ, primeExponentP, primeExponentQ, crtCoefficient));

			return pair;
		} catch (SshException e) {
			throw new SshIOException(e);
		}

	}

	SshKeyPair getDSAKeyPair(SimpleASNReader asn) throws IOException {

		try {
			asn.assertByte(0x30); // SEQUENCE
			asn.getLength();

			asn.assertByte(0x02); // INTEGER (version)
			asn.getData();

			asn.assertByte(0x02); // INTEGER (p)
			BigInteger p = new BigInteger(asn.getData());

			asn.assertByte(0x02); // INTEGER (q)
			BigInteger q = new BigInteger(asn.getData());

			asn.assertByte(0x02); // INTEGER (g)
			BigInteger g = new BigInteger(asn.getData());

			asn.assertByte(0x02); // INTEGER (y)
			BigInteger y = new BigInteger(asn.getData());

			asn.assertByte(0x02); // INTEGER (x)
			BigInteger x = new BigInteger(asn.getData());

			SshKeyPair pair = new SshKeyPair();
			SshDsaPublicKey pub = ComponentManager.getInstance().createDsaPublicKey(p, q, g, y);
			pair.setPublicKey(pub);

			pair.setPrivateKey(ComponentManager.getInstance().createDsaPrivateKey(p, q, g, x, pub.getY()));

			return pair;
		} catch (SshException e) {
			throw new SshIOException(e);
		}
	}

	void writeECDSAKeyPair(SimpleASNWriter seq, Ssh2EcdsaSha2NistPrivateKey privatekey,
			Ssh2EcdsaSha2NistPublicKey publickey) {

		SimpleASNWriter asn = new SimpleASNWriter();

		asn.writeByte(0x02);

		byte[] version = new byte[1];
		asn.writeData(version);

		asn.writeByte(0x04);
		asn.writeData(((ECPrivateKey) privatekey.getJCEPrivateKey()).getS().toByteArray());

		asn.writeByte(0xA0);

		SimpleASNWriter oid = new SimpleASNWriter();
		oid.writeByte(0x06);
		oid.writeData(publickey.getOid());
		byte[] oidBytes = oid.toByteArray();
		asn.writeData(oidBytes);

		asn.writeByte(0xA1);
		SimpleASNWriter pk = new SimpleASNWriter();
		pk.writeByte(0x03);
		byte[] pub = publickey.getPublicOctet();
		pk.writeLength(pub.length + 1);
		pk.writeByte(0);
		pk.write(pub);
		byte[] pkBytes = pk.toByteArray();
		asn.writeData(pkBytes);

		seq.writeByte(0x30);
		seq.writeData(asn.toByteArray());

	}

	void writeDSAKeyPair(SimpleASNWriter asn, SshDsaPrivateKey privatekey, SshDsaPublicKey publickey) {
		// Write to a substream temporarily.
		// This code needs to know the length of the substream before it can write the
		// data from
		// the substream to the main stream.
		SimpleASNWriter asn2 = new SimpleASNWriter();

		asn2.writeByte(0x02); // INTEGER (version)

		byte[] version = new byte[1];
		asn2.writeData(version);
		asn2.writeByte(0x02); // INTEGER (p)
		asn2.writeData(publickey.getP().toByteArray());
		asn2.writeByte(0x02); // INTEGER (q)
		asn2.writeData(publickey.getQ().toByteArray());
		asn2.writeByte(0x02); // INTEGER (g)
		asn2.writeData(publickey.getG().toByteArray());
		asn2.writeByte(0x02); // INTEGER (y)
		asn2.writeData(publickey.getY().toByteArray());
		asn2.writeByte(0x02); // INTEGER (x)
		asn2.writeData(privatekey.getX().toByteArray());

		byte[] dsaKeyEncoded = asn2.toByteArray();

		asn.writeByte(0x30); // SEQUENCE
		asn.writeData(dsaKeyEncoded);
	}

	void writeRSAKeyPair(SimpleASNWriter asn, SshRsaPrivateCrtKey privatekey) {
		// Write to a substream temporarily.
		// This code needs to know the length of the substream before it can write the
		// data from
		// the substream to the main stream.
		SimpleASNWriter asn2 = new SimpleASNWriter();

		asn2.writeByte(0x02); // INTEGER (version)

		byte[] version = new byte[1];
		asn2.writeData(version);
		asn2.writeByte(0x02); // INTEGER ()
		asn2.writeData(privatekey.getModulus().toByteArray());
		asn2.writeByte(0x02); // INTEGER ()
		asn2.writeData(privatekey.getPublicExponent().toByteArray());
		asn2.writeByte(0x02); // INTEGER ()
		asn2.writeData(privatekey.getPrivateExponent().toByteArray());
		asn2.writeByte(0x02); // INTEGER ()
		asn2.writeData(privatekey.getPrimeP().toByteArray());
		asn2.writeByte(0x02); // INTEGER ()
		asn2.writeData(privatekey.getPrimeQ().toByteArray());
		asn2.writeByte(0x02); // INTEGER ()
		asn2.writeData(privatekey.getPrimeExponentP().toByteArray());
		asn2.writeByte(0x02); // INTEGER ()
		asn2.writeData(privatekey.getPrimeExponentQ().toByteArray());
		asn2.writeByte(0x02); // INTEGER ()
		asn2.writeData(privatekey.getCrtCoefficient().toByteArray());

		byte[] rsaKeyEncoded = asn2.toByteArray();

		asn.writeByte(0x30); // SEQUENCE
		asn.writeData(rsaKeyEncoded);
	}

	public byte[] encryptKey(SshKeyPair pair, String passphrase) throws IOException {

		try (ByteArrayWriter writer = new ByteArrayWriter()) {
			StringWriter w = new StringWriter();
			PEMWriter pem = new PEMWriter();
			pem.setType(PEM.OPENSSH_PRIVATE_KEY);
			writeOpenSSHKeyPair(writer, pair, passphrase);
			pem.write(w, writer.toByteArray());

			return w.toString().getBytes("UTF-8");
		} catch (NoSuchAlgorithmException | SshException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sshtools.publickey.SshPrivateKeyFile#changePassphrase(java.lang.String,
	 * java.lang.String)
	 */
	public void changePassphrase(String oldpassphrase, String newpassphrase)
			throws IOException, InvalidPassphraseException {
		SshKeyPair pair = toKeyPair(oldpassphrase);
		formattedkey = encryptKey(pair, newpassphrase);
	}

	public byte[] getFormattedKey() {
		return formattedkey;
	}

	public static boolean isFormatted(byte[] formattedkey) {
		try {
			Reader r = new StringReader(new String(formattedkey, "UTF-8"));
//   PEMReader pem = 
			new PEMReader(r);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}