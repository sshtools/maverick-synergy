package com.sshtools.common.publickey;

/*-
 * #%L
 * Base API
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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.StringTokenizer;

import javax.crypto.Cipher;

import com.sshtools.common.ssh.components.SshRsaPrivateKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.util.Base64;

public class RsaUtils {

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
