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

package com.sshtools.common.ssh.components.jce;

/**
 * Interface containing the JCE algorithms required by the API. 
 * @author Lee David Painter
 *
 */
public interface JCEAlgorithms {

	/** Secure random algorithm 'Sha1PRNG' **/
	public static final String JCE_SHA1PRNG = "SHA1PRNG";
	/** RSA public key algorithm 'RSA' **/
	public static final String JCE_RSA = "RSA";
	/** DSA public key algorithm 'DSA' **/
	public static final String JCE_DSA = "DSA";
	/** RSA signature algorithm 'SHA1WithRSA' **/
	public static final String JCE_SHA1WithRSA = "SHA1WithRSA";
	/** RSA signature algorithm 'SHA256WithRSA' **/
	public static final String JCE_SHA256WithRSA = "SHA256WithRSA";
	/** RSA signature algorithm 'SHA512WithRSA' **/
	public static final String JCE_SHA512WithRSA = "SHA512WithRSA";
	/** RSA signature algorithm 'SHA1WithRSA' **/
	public static final String JCE_MD5WithRSA = "MD5WithRSA";
	/** DSA signature algorithm 'SHA1WithDSA' **/
	public static final String JCE_SHA1WithDSA = "SHA1WithDSA";
	/** MD5 digest algorithm 'MD5' **/
	public static final String JCE_MD5 = "MD5";
	/** SHA1 digest algorithm 'SHA-1' **/
	public static final String JCE_SHA1 = "SHA-1";
	/** SHA256 digest algorithm 'SHA-256' **/
	public static final String JCE_SHA256 = "SHA-256";
	/** SHA384 digest algorithm 'SHA-384' **/
	public static final String JCE_SHA384 = "SHA-384";
	/** SHA512 digest algorithm 'SHA-512' **/
	public static final String JCE_SHA512 = "SHA-512";
	/** ECDSA signature algorithm **/
	public static final String JCE_SHA1WithECDSA = "SHA1withECDSA";
	/** ECDSA signature algorithm **/
	public static final String JCE_SHA256WithECDSA = "SHA256withECDSA";
	/** ECDSA signature algorithm **/
	public static final String JCE_SHA384WithECDSA = "SHA384withECDSA";
	/** ECDSA signature algorithm **/
	public static final String JCE_SHA512WithECDSA = "SHA512withECDSA";
	
	/** AES encryption algorithm 'AES/CBC/NoPadding' **/
	public static final String JCE_AESCBCNOPADDING = "AES/CBC/NoPadding";
	/** Blowfish encryption algorithm 'Blowfish/CBC/NoPadding' **/
	public static final String JCE_BLOWFISHCBCNOPADDING = "Blowfish/CBC/NoPadding";
	
	/** Diffie Hellman key agreement algorithm 'DH' **/
	public static final String JCE_DH = "DH";
	
	public static final String JCE_DH_KEY_FACTORY = "DH_KeyFactory";
	public static final String JCE_DH_KEY_AGREEMENT = "DH_KeyAgreement";
	public static final String JCE_DH_KEY_GENERATOR = "DH_KeyGenerator";
	
	/** MD5 message authentication code algorithm 'HmacMD5' **/
	public static final String JCE_HMACMD5 = "HmacMD5";
	/** SHA1 message authentication code algorithm 'HmacSha1' **/
	public static final String JCE_HMACSHA1 = "HmacSha1";
	/** SHA 256 bit message authentication code algorithm 'HmacSha256' **/
	public static final String JCE_HMACSHA256 = "HmacSha256";
	/** SHA 512 bit message authentication code algorithm 'HmacSha256' **/
	public static final String JCE_HMACSHA512 = "HmacSha512";
	/** RipeMD160 message authentication code algorithm 'HmacSha256' **/
	public static final String JCE_HMACRIPEMD160 = "HmacRipeMd160";
	
	/** DES encrpytion algorithm 'DES/CBC/NoPadding' **/
	public static final String JCE_DESCBCNOPADDING = "DES/CBC/NoPadding";
	/** RSA encryption algorithm 'RSA/NONE/PKCS1Padding' **/
	public static final String JCE_RSANONEPKCS1PADDING = "RSA";
	/** X509 certificate algorithm 'X.509' **/
	public static final String JCE_X509 = "X.509";
	
	/** AES in counter clock mode 'AES/CTR/NoPadding' **/
	public static final String JCE_AESCTRNOPADDING = "AES/CTR/NoPadding";
	
	/** AES in galois counter mode 'AES/CTR/NoPadding' **/
	public static final String JCE_AESGCMNOPADDING = "AES/GCM/NoPadding";
	
	/** 3DES in counter clock mode 'DESede/CTR/NoPadding' **/
	public static final String JCE_3DESCTRNOPADDING = "DESede/CTR/NoPadding";

	/** 3DES in CBC mode 'DESede/CTR/NoPadding' **/
	public static final String JCE_3DESCBCNOPADDING = "DESede/CBC/NoPadding";

	/** ARCFOUR cipher **/
	public static final String JCE_ARCFOUR = "ARCFOUR";

	/** Elliptic Curve DSA **/
	
	/** Elliptic Curve Diffie Hellmam **/
	public static final String JCE_ECDH = "ECDH";
	public static final String JCE_RSA_CIPHER = "RSA_Cipher";
	public static final String EdDSA = "EdDSA";
	public static final String ED25519 = "Ed25519";
	
}
