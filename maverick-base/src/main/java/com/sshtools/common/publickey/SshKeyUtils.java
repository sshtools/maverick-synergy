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
package com.sshtools.common.publickey;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA256;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA512;
import com.sshtools.common.util.IOUtils;

public class SshKeyUtils {

	public static String getOpenSSHFormattedKey(SshPublicKey key) throws IOException {
		return getFormattedKey(key, "", SshPublicKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static String getOpenSSHFormattedKey(SshPublicKey key, String comment) throws IOException {
		return getFormattedKey(key, comment, SshPublicKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static String getFormattedKey(SshPublicKey key, String comment, int format) throws IOException {	
		SshPublicKeyFile file = SshPublicKeyFileFactory.create(key, comment, format);
		return new String(file.getFormattedKey(), "UTF-8");
	}
	
	public static String getFormattedKey(SshPublicKey key, String comment) throws IOException {	
		SshPublicKeyFile file = SshPublicKeyFileFactory.create(key, comment, SshPublicKeyFileFactory.OPENSSH_FORMAT);
		return new String(file.getFormattedKey(), "UTF-8");
	}
	
	public static String getFormattedKey(SshKeyPair pair, String passphrase) throws IOException {	
		SshPrivateKeyFile kf = SshPrivateKeyFileFactory.create(pair, passphrase);
		return new String(kf.getFormattedKey(), "UTF-8");
	}
	
	public static SshPublicKey getPublicKey(File key) throws IOException {
		return getPublicKey(toString(new FileInputStream(key)));
	}
	
	public static SshPublicKey getPublicKey(InputStream key) throws IOException {
		return getPublicKey(toString(key));
	}
	
	public static SshPublicKey getPublicKey(String formattedKey) throws IOException {
		SshPublicKeyFile file = SshPublicKeyFileFactory.parse(formattedKey.getBytes("UTF-8"));
		return file.toPublicKey();
	}
	
	public static String getPublicKeyComment(String formattedKey) throws IOException {
		SshPublicKeyFile file = SshPublicKeyFileFactory.parse(formattedKey.getBytes("UTF-8"));
		return file.getComment();
	}
	
	public static SshKeyPair getPrivateKey(File key, String passphrase) throws IOException, InvalidPassphraseException {
		return getPrivateKey(toString(new FileInputStream(key)), passphrase);
	}
	
	public static SshKeyPair getPrivateKey(InputStream key, String passphrase) throws IOException, InvalidPassphraseException {
		return getPrivateKey(toString(key), passphrase);
	}
	
	public static SshKeyPair getPrivateKey(String formattedKey, String passphrase) throws IOException, InvalidPassphraseException {
		SshPrivateKeyFile file = SshPrivateKeyFileFactory.parse(formattedKey.getBytes("UTF-8"));
		return file.toKeyPair(passphrase);
	}

	public static SshKeyPair makeRSAWithSHA256Signature(SshKeyPair pair) {
		SshKeyPair n = new SshKeyPair();
		n.setPrivateKey(pair.getPrivateKey());
		n.setPublicKey(new Ssh2RsaPublicKeySHA256((SshRsaPublicKey)pair.getPublicKey()));
		return n;
	}
	
	public static SshKeyPair getRSAPrivateKeyWithSHA256Signature(String formattedKey, String passphrase) throws UnsupportedEncodingException, IOException, InvalidPassphraseException {
		SshPrivateKeyFile file = SshPrivateKeyFileFactory.parse(formattedKey.getBytes("UTF-8"));
		return makeRSAWithSHA256Signature(file.toKeyPair(passphrase));
	}

	public static SshKeyPair getRSAPrivateKeyWithSHA256Signature(InputStream key, String passphrase) throws IOException, InvalidPassphraseException {
		return makeRSAWithSHA256Signature(getPrivateKey(key, passphrase));
	}
	
	public static SshKeyPair getRSAPrivateKeyWithSHA256Signature(File key, String passphrase) throws IOException, InvalidPassphraseException {
		return makeRSAWithSHA256Signature(getPrivateKey(key, passphrase));
	}
	
	public static SshKeyPair makeRSAWithSHA512Signature(SshKeyPair pair) {
		SshKeyPair n = new SshKeyPair();
		n.setPrivateKey(pair.getPrivateKey());
		n.setPublicKey(new Ssh2RsaPublicKeySHA512((SshRsaPublicKey)pair.getPublicKey()));
		return n;
	}
	
	public static SshKeyPair getRSAPrivateKeyWithSHA512Signature(String formattedKey, String passphrase) throws UnsupportedEncodingException, IOException, InvalidPassphraseException {
		SshPrivateKeyFile file = SshPrivateKeyFileFactory.parse(formattedKey.getBytes("UTF-8"));
		return makeRSAWithSHA512Signature(file.toKeyPair(passphrase));
	}

	public static SshKeyPair getRSAPrivateKeyWithSHA512Signature(InputStream key, String passphrase) throws IOException, InvalidPassphraseException {
		return makeRSAWithSHA512Signature(getPrivateKey(key, passphrase));
	}
	
	public static SshKeyPair getRSAPrivateKeyWithSHA512Signature(File key, String passphrase) throws IOException, InvalidPassphraseException {
		return makeRSAWithSHA512Signature(getPrivateKey(key, passphrase));
	}
	
	public static String getFingerprint(SshPublicKey key) {
		return SshKeyFingerprint.getFingerprint(key);
	}
	
	public static String getBubbleBabble(SshPublicKey pub) {
		return SshKeyFingerprint.getBubbleBabble(pub);
	}
	
	private static String toString(InputStream in) throws IOException {
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			IOUtils.copy(in, out);
			IOUtils.closeStream(in);
		
			return new String(out.toByteArray(), "UTF-8");
		} finally {
			IOUtils.closeStream(out);
		}
	}	
	public static void createPublicKeyFile(SshPublicKey publicKey, String comment, File file) throws IOException {
		createPublicKeyFile(publicKey, comment, file, SshPublicKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static void createPublicKeyFile(SshPublicKey publicKey, String comment, File file, int format) throws IOException {
		
		SshPublicKeyFile kf = SshPublicKeyFileFactory.create(publicKey, comment, format);
		IOUtils.writeUTF8StringToFile(file, new String(kf.getFormattedKey(), "UTF-8"));
		
	}

	public static void createPrivateKeyFile(SshKeyPair pair, String passphrase, File file) throws IOException {
		createPrivateKeyFile(pair, passphrase, file, SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static void createPrivateKeyFile(SshKeyPair pair, String passphrase, File file, int format) throws IOException {
		
		SshPrivateKeyFile kf = SshPrivateKeyFileFactory.create(pair, passphrase, format);
		IOUtils.writeUTF8StringToFile(file, new String(kf.getFormattedKey(), "UTF-8"));
	}

	public static boolean isPrivateKeyFile(File file) {
		
		try(InputStream in = new FileInputStream(file)) {
			SshPrivateKeyFileFactory.parse(in);
			return true;
		} catch (IOException e) {

		}
		return false;
	}
	
	public static SshKeyPair getCertificateAndKey(File privateKey, String passphrase) throws IOException, InvalidPassphraseException {
		File certFile = new File(privateKey.getAbsolutePath() + "-cert.pub");
		if(!certFile.exists()) {
			throw new IOException(String.format("No certificate file %s to match private key file %s", certFile.getName(), privateKey.getName()));
		}
		SshKeyPair pair = getPrivateKey(privateKey, passphrase);
		pair.setPublicKey(getPublicKey(certFile));
		return pair;
	}

}
