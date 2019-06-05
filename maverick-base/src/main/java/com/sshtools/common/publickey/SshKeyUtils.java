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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.publickey;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.StringTokenizer;

import javax.crypto.Cipher;

import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPrivateKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA256;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA512;
import com.sshtools.common.util.Base64;
import com.sshtools.common.util.IOUtil;

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
			IOUtil.copy(in, out);
			IOUtil.closeStream(in);
		
			return new String(out.toByteArray(), "UTF-8");
		} finally {
			IOUtil.closeStream(out);
		}
	}	
	
	public static String encrypt(SshRsaPrivateKey privateKey, String toEncrypt) throws Exception {
		int pos = 0;
		StringBuffer ret = new StringBuffer();
		int blockLength = privateKey.getModulus().bitLength() / 16;
		while(pos < toEncrypt.length()) {
			int count = Math.min(toEncrypt.length() - pos, blockLength);
			ret.append(doEncrypt(toEncrypt.substring(pos, pos+count), privateKey.getJCEPrivateKey()));
			ret.append('|');
			pos += count;
		}
		return ret.toString();
	}
	
	public static String decrypt(SshRsaPublicKey publicKey, String toDecrypt) throws Exception {
		StringBuffer ret = new StringBuffer();
		StringTokenizer t = new StringTokenizer(toDecrypt, "|");
		
		while(t.hasMoreTokens()) {
		
			String data = t.nextToken();
			ret.append(doDecrypt(data, publicKey.getJCEPublicKey()));
		}

		return ret.toString();
	}

	private static String doEncrypt(String toEncrypt, PrivateKey privateKey) throws Exception{

		Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		c.init(Cipher.ENCRYPT_MODE, privateKey);
		return Base64.encodeBytes(c.doFinal(toEncrypt.getBytes("UTF-8")), true);
		
	}
	

	private static String doDecrypt(String toDecrypt, PublicKey publicKey) throws Exception {
		
		Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		c.init(Cipher.DECRYPT_MODE, publicKey);
		return new String(c.doFinal(Base64.decode(toDecrypt)), "UTF-8");
	}
	
	public static String encrypt(SshRsaPublicKey publicKey, String toEncrypt) throws Exception {
		int pos = 0;
		StringBuffer ret = new StringBuffer();
		int blockLength = publicKey.getModulus().bitLength() / 16;
		while(pos < toEncrypt.length()) {
			int count = Math.min(toEncrypt.length() - pos, blockLength);
			ret.append(doEncrypt(toEncrypt.substring(pos, pos+count), publicKey.getJCEPublicKey()));
			ret.append('|');
			pos += count;
		}
		return ret.toString();
	}
	
	public static String decrypt(SshRsaPrivateKey privateKey, String toDecrypt) throws Exception {
		StringBuffer ret = new StringBuffer();
		StringTokenizer t = new StringTokenizer(toDecrypt, "|");
		
		while(t.hasMoreTokens()) {
		
			String data = t.nextToken();
			ret.append(doDecrypt(data, privateKey.getJCEPrivateKey()));
		}

		return ret.toString();
	}

	private static String doEncrypt(String toEncrypt, PublicKey publicKey) throws Exception{

		Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		c.init(Cipher.ENCRYPT_MODE, publicKey);
		return Base64.encodeBytes(c.doFinal(toEncrypt.getBytes("UTF-8")), true);
		
	}
	

	private static String doDecrypt(String toDecrypt, PrivateKey privateKey) throws Exception {
		
		Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		c.init(Cipher.DECRYPT_MODE, privateKey);
		return new String(c.doFinal(Base64.decode(toDecrypt)), "UTF-8");
	}
	
	public static String encryptOAEP(SshRsaPublicKey publicKey, String toEncrypt) throws Exception {
		int pos = 0;
		StringBuffer ret = new StringBuffer();
		int blockLength = publicKey.getModulus().bitLength() / 16;
		while(pos < toEncrypt.length()) {
			int count = Math.min(toEncrypt.length() - pos, blockLength);
			ret.append(doOAEPSHA256Encrypt(toEncrypt.substring(pos, pos+count), publicKey.getJCEPublicKey()));
			ret.append('|');
			pos += count;
		}
		return ret.toString();
	}
	
	public static String decryptOAEP(SshRsaPrivateKey privateKey, String toDecrypt) throws Exception {
		StringBuffer ret = new StringBuffer();
		StringTokenizer t = new StringTokenizer(toDecrypt, "|");
		
		while(t.hasMoreTokens()) {
		
			String data = t.nextToken();
			ret.append(doOAEPSHA256Decrypt(data, privateKey.getJCEPrivateKey()));
		}

		return ret.toString();
	}
	
	
	private static String doOAEPSHA256Encrypt(String toEncrypt, PublicKey publicKey) throws Exception{

		Cipher c = Cipher.getInstance("RSA/None/OAEPWithSHA256AndMGF1Padding");
		c.init(Cipher.ENCRYPT_MODE, publicKey);
		return Base64.encodeBytes(c.doFinal(toEncrypt.getBytes("UTF-8")), true);
		
	}
	

	private static String doOAEPSHA256Decrypt(String toDecrypt, PrivateKey privateKey) throws Exception {
		
		Cipher c = Cipher.getInstance("RSA/None/OAEPWithSHA256AndMGF1Padding");
		c.init(Cipher.DECRYPT_MODE, privateKey);
		return new String(c.doFinal(Base64.decode(toDecrypt)), "UTF-8");
	}

}
