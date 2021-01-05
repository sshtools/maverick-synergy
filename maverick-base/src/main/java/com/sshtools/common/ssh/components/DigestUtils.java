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
package com.sshtools.common.ssh.components;

import java.io.UnsupportedEncodingException;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.util.Utils;

public class DigestUtils {

	
	public static byte[] digest(String name, byte[] b) {
		
		try {
			Digest digest = JCEComponentManager.getDefaultInstance().getDigest(name);
			digest.putBytes(b);
			return digest.doFinal();
		} catch (SshException e) {
			throw new IllegalArgumentException(String.format("%s is not a supported digest", name));
		}
	}
	
	public static byte[] md5(byte[] b) {
		return digest(JCEAlgorithms.JCE_MD5, b);
	}
	
	public static byte[] sha1(byte[] b) {
		return digest(JCEAlgorithms.JCE_SHA1, b);
	}
	
	public static byte[] sha256(byte[] b) {
		return digest(JCEAlgorithms.JCE_SHA256, b);
	}
	
	public static byte[] sha384(byte[] b) {
		return digest(JCEAlgorithms.JCE_SHA384, b);
	}
	
	public static byte[] sha512(byte[] b) {
		return digest(JCEAlgorithms.JCE_SHA512, b);
	}

	public static String sha1Hex(String string) {
		try {
			return sha1Hex(string.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Encoding error. This system does not support UTF-8!");
		}
	}
	
	public static String sha1Hex(byte[] b) {
		return Utils.bytesToHex(sha1(b));
	}
	
	public static String sha256Hex(String string) {
		try {
			return sha256Hex(string.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Encoding error. This system does not support UTF-8!");
		}
	}
	
	public static String sha256Hex(byte[] b) {
		return Utils.bytesToHex(sha256(b));
	}
	
	public static String sha384Hex(String string) {
		try {
			return sha384Hex(string.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Encoding error. This system does not support UTF-8!");
		}
	}
	
	public static String sha384Hex(byte[] b) {
		return Utils.bytesToHex(sha384(b));
	}
	
	public static String sha512Hex(String string) {
		try {
			return sha512Hex(string.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Encoding error. This system does not support UTF-8!");
		}
	}
	
	public static String sha512Hex(byte[] b) {
		return Utils.bytesToHex(sha512(b));
	}
}
