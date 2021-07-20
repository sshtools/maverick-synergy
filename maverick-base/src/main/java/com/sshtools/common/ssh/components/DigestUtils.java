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
