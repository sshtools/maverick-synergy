/* 
 * Copyright (c) 2018 SSHTools - Ported to Java
 * 
 * Based on http://cvsweb.openbsd.org/cgi-bin/cvsweb/src/lib/libutil/bcrypt_pbkdf.c?rev=1.13&content-type=text/x-cvsweb-markup
 * 
 * Useful testing webapp: https://asecuritysite.com/encryption/PBKDF2z
 * 
 * Other interesting reading: RFC 2898
 * 
 * Copyright (c) 2013 Ted Unangst <tedu@openbsd.org>
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.sshtools.common.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BCryptKDF {
	final static int BCRYPT_WORDS = 8;
	final static int BCRYPT_HASHSIZE = BCRYPT_WORDS * 4;
	
	static byte[] bcrypt_hash(byte[] sha2pass, byte[] sha2salt)
	{
		BCrypt B = new BCrypt();

		byte[] out = new byte[BCRYPT_HASHSIZE];
		byte[] ciphertext;
		try {
			ciphertext = "OxychromaticBlowfishSwatDynamite".getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("ASCII not supported :\\");
		}
		int[] cdata = new int[BCRYPT_WORDS];
		int i;

		B.init_key(); // OK
		B.ekskey(sha2salt, sha2pass); // OK
		for (i = 0; i < 64; i++) {
			B.key(sha2salt); // OK
			B.key(sha2pass); // OK
		}

		/* encryption */
		int[] j = new int[] { 0 };
		for (i = 0; i < BCRYPT_WORDS; i++) {
			cdata[i] = BCrypt.streamtoword(ciphertext, j);
		}
		for (i = 0; i < 64; i++) {
			B.blf_enc(cdata, BCRYPT_HASHSIZE / BCRYPT_WORDS);
		}

		/* copy out */
		for (i = 0; i < cdata.length; i++) {
			out[4 * i + 3] = (byte)((cdata[i] >> 24) & 0xff);
			out[4 * i + 2] = (byte)((cdata[i] >> 16) & 0xff);
			out[4 * i + 1] = (byte)((cdata[i] >> 8) & 0xff);
			out[4 * i + 0] = (byte)(cdata[i] & 0xff);
		}
		
		return out;

	}

	public static byte[] bcrypt_pbkdf(byte[] pass, byte[] salt, int keylen, int rounds)
			throws NoSuchAlgorithmException {
		byte[] sha2salt;
		byte[] out = new byte[BCRYPT_HASHSIZE];
		byte[] tmpout = new byte[BCRYPT_HASHSIZE];
		byte[] countsalt = new byte[4];
		int i, j, amt;
		int count;
		byte[] key = new byte[keylen];
		int origkeylen = keylen;

		if (rounds < 1)
			throw new IllegalArgumentException("Not enough rounds.");

		if (pass.length == 0 || salt.length == 0 || keylen == 0 || keylen > (out.length * out.length))
			throw new IllegalArgumentException("Invalid pass, salt or key.");

		int stride = (keylen + out.length - 1) / out.length;
		amt = (keylen + stride - 1) / stride;

		/* collapse password */
		MessageDigest ctx = MessageDigest.getInstance("SHA-512");
		ctx.update(pass);
		byte[] sha2pass = ctx.digest();

		/* generate key, sizeof(out) at a time */
		for (count = 1; keylen > 0; count++) {
			countsalt[0] = (byte) ((count >> 24) & 0xff);
			countsalt[1] = (byte) ((count >> 16) & 0xff);
			countsalt[2] = (byte) ((count >> 8) & 0xff);
			countsalt[3] = (byte) (count & 0xff);

			/* first round, salt is salt */
			ctx.reset();
			ctx.update(salt);
			ctx.update(countsalt);
			sha2salt = ctx.digest();

			tmpout = bcrypt_hash(sha2pass, sha2salt);
			System.arraycopy(tmpout, 0, out, 0, out.length);

			for (i = 1; i < rounds; i++) {
				/* subsequent rounds, salt is previous output */
				ctx.reset();
				ctx.update(tmpout);
				sha2salt = ctx.digest();
				tmpout = bcrypt_hash(sha2pass, sha2salt);
				for (j = 0; j < out.length; j++)
					out[j] ^= tmpout[j];
			}

			/*
			 * pbkdf2 deviation: output the key material non-linearly.
			 */
			amt = Math.min(amt, keylen);
			for (i = 0; i < amt; i++) {
				int dest = i * stride + (count - 1);
				if (dest >= origkeylen)
					break;
				key[dest] = out[i];
			}
			keylen -= i;
		}

		return key;
	}
}
