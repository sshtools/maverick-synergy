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
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Hashtable;

import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.jce.AES128Cbc;
import com.sshtools.common.ssh.components.jce.TripleDesCbc;
import com.sshtools.common.util.Base64;

public class PEMReader extends PEM {
	private LineNumberReader reader;
	private String type;
	private Hashtable<String, String> header;
	private byte[] payload;

	/**
	 * Creates a new PEMReader object.
	 * 
	 * @param r
	 */
	public PEMReader(Reader r) throws IOException {
		reader = new LineNumberReader(r);
		read();
	}

	private void read() throws IOException {
		String line;

		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.startsWith(PEM_BOUNDARY) && line.endsWith(PEM_BOUNDARY)) {
				if (line.startsWith(PEM_BEGIN)) {
					type = line.substring(PEM_BEGIN.length(), line.length()
							- PEM_BOUNDARY.length());

					break;
				}
				throw new IOException("Invalid PEM boundary at line "
						+ reader.getLineNumber() + ": " + line);
			}
		}

		header = new Hashtable<String, String>();

		while ((line = reader.readLine()) != null) {
			int colon = line.indexOf(':');

			if (colon == -1) {
				break;
			}

			String key = line.substring(0, colon).trim();

			if (line.endsWith("\\")) {
				String v = line.substring(colon + 1, line.length() - 1).trim();

				// multi-line value
				StringBuffer value = new StringBuffer(v);

				while ((line = reader.readLine()) != null) {
					if (line.endsWith("\\")) {
						value.append(" ").append(
								line.substring(0, line.length() - 1).trim());
					} else {
						value.append(" ").append(line.trim());

						break;
					}
				}
			} else {
				String value = line.substring(colon + 1).trim();
				header.put(key, value);
			}
		}

		// first line that is not part of the header
		// could be an empty line, but if there is no header and the body begins
		// straight after the -----
		// then this line contains data

		if (line == null) {
			throw new IOException(
					"The key format is invalid! OpenSSH formatted keys must begin with -----BEGIN RSA or -----BEGIN DSA");
		}

		StringBuffer body = new StringBuffer(line);

		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.startsWith(PEM_BOUNDARY) && line.endsWith(PEM_BOUNDARY)) {
				if (line.startsWith(PEM_END + type)) {
					break;
				}
				throw new IOException("Invalid PEM end boundary at line "
						+ reader.getLineNumber() + ": " + line);
			}

			body.append(line);
		}

		payload = Base64.decode(body.toString());
	}

	/**
	 * 
	 * 
	 * @return Hashtable
	 */
	public Hashtable<String, String> getHeader() {
		return header;
	}

	/**
	 * 
	 * 
	 * @return byte[]
	 */
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * 
	 * 
	 * @return String
	 */
	public String getType() {
		return type;
	}

	/**
	 * 
	 * 
	 * @param passphrase
	 * 
	 * @return byte[]
	 * 
	 */
	public byte[] decryptPayload(String passphrase) throws IOException {

		String dekInfo = (String) header.get("DEK-Info");

		if (dekInfo != null) {
			int comma = dekInfo.indexOf(',');
			String keyAlgorithm = dekInfo.substring(0, comma);

			if (!"DES-EDE3-CBC".equalsIgnoreCase(keyAlgorithm)
					&& !"AES-128-CBC".equalsIgnoreCase(keyAlgorithm)) {
				throw new IOException("Unsupported passphrase algorithm: "
						+ keyAlgorithm);
			}

			String ivString = dekInfo.substring(comma + 1);
			byte[] iv = new byte[ivString.length() / 2];

			for (int i = 0; i < ivString.length(); i += 2) {
				iv[i / 2] = (byte) Integer.parseInt(
						ivString.substring(i, i + 2), 16);
			}

			byte[] keydata = null;
			SshCipher cipher = null;

			if ("DES-EDE3-CBC".equalsIgnoreCase(keyAlgorithm)) {
				keydata = getKeyFromPassphrase(passphrase, iv, 24);
				cipher = new TripleDesCbc();
			} else if ("AES-128-CBC".equalsIgnoreCase(keyAlgorithm)) {
				keydata = getKeyFromPassphrase(passphrase, iv, 16);
				cipher = new AES128Cbc();
			}

			cipher.init(SshCipher.DECRYPT_MODE, iv, keydata);

			byte[] plain = new byte[payload.length];
			cipher.transform(payload, 0, plain, 0, plain.length);

			return plain;
		}
		return payload;
	}
}
