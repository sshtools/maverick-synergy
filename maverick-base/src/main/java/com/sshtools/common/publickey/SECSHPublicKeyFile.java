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

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class SECSHPublicKeyFile extends Base64EncodedFileFormat implements
		SshPublicKeyFile {

	static String BEGIN = "---- BEGIN SSH2 PUBLIC KEY ----";
	static String END = "---- END SSH2 PUBLIC KEY ----";
	String algorithm;
	byte[] encoded;

	SECSHPublicKeyFile(byte[] formattedkey) throws IOException {
		super(BEGIN, END);
		encoded = getKeyBlob(formattedkey);
		toPublicKey(); // Validate
	}

	SECSHPublicKeyFile(SshPublicKey key, String comment) throws IOException {
		super(BEGIN, END);
		try {
			algorithm = key.getAlgorithm();
			encoded = key.getEncoded();
			setComment(comment);
			toPublicKey(); // Validate
		} catch (SshException ex) {
			throw new IOException("Failed to encode public key");
		}
	}

	public String getComment() {
		return getHeaderValue("Comment");
	}

	public SshPublicKey toPublicKey() throws IOException {
		return SshPublicKeyFileFactory.decodeSSH2PublicKey(encoded);
	}

	public byte[] getFormattedKey() throws IOException {
		return formatKey(encoded);
	}

	public void setComment(String comment) {
		setHeaderValue("Comment", (comment.trim().startsWith("\"") ? "" : "\"")
				+ comment.trim() + (comment.trim().endsWith("\"") ? "" : "\""));
	}

	public String toString() {
		try {
			return new String(getFormattedKey(), "UTF-8");
		} catch (IOException ex) {
			return "Invalid encoding!";
		}

	}

	public String getOptions() {
		return null;
	}

}
