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
