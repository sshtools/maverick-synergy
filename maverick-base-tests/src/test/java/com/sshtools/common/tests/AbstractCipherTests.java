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

package com.sshtools.common.tests;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.junit.Ignore;

import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.jce.AES128Cbc;
import com.sshtools.common.ssh.components.jce.AES128Ctr;
import com.sshtools.common.ssh.components.jce.AES192Cbc;
import com.sshtools.common.ssh.components.jce.AES192Ctr;
import com.sshtools.common.ssh.components.jce.AES256Cbc;
import com.sshtools.common.ssh.components.jce.AES256Ctr;
import com.sshtools.common.ssh.components.jce.ArcFour128;
import com.sshtools.common.ssh.components.jce.ArcFour256;
import com.sshtools.common.ssh.components.jce.BlowfishCbc;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.TripleDesCbc;
import com.sshtools.common.ssh.components.jce.TripleDesCtr;
import com.sshtools.common.util.Arrays;

import junit.framework.TestCase;

@Ignore
public abstract class AbstractCipherTests extends TestCase {

	protected abstract String getTestingJCE();
	
	protected void testCipher(SshCipher encrypt, SshCipher decrypt) throws IOException, NoSuchAlgorithmException {
		
		assertEquals("Cipher not using correct JCE", getTestingJCE(), encrypt.getProviderName());
		assertEquals("Cipher not using correct JCE", getTestingJCE(), decrypt.getProviderName());
		
		byte[] key = new byte[encrypt.getKeyLength()];
		byte[] iv = new byte[encrypt.getBlockSize()];
		int msglen = (encrypt.getKeyLength() * 8);
		byte[] data = new byte[msglen];
		byte[] cipherText = new byte[data.length];
		
		JCEComponentManager.getSecureRandom().nextBytes(key);
		JCEComponentManager.getSecureRandom().nextBytes(iv);
		JCEComponentManager.getSecureRandom().nextBytes(data);
		
		encrypt.init(SshCipher.ENCRYPT_MODE, iv, key);
		decrypt.init(SshCipher.DECRYPT_MODE, iv, key);
		
		for(int i=0;i<100000;i++) {
			encrypt.transform(data, 0, cipherText, 0, msglen);
			decrypt.transform(cipherText, 0, cipherText, 0, msglen);		
			assertTrue("Encrypt/Decrypt failure", Arrays.areEqual(data,  cipherText));
		}
		
	}
	
	public void testAES128bitCBC() throws NoSuchAlgorithmException, IOException {
		testCipher(new AES128Cbc(), new AES128Cbc());
	}
	
	public void testAES192bitCBC() throws NoSuchAlgorithmException, IOException {
		testCipher(new AES192Cbc(), new AES192Cbc());
	}
	
	public void testAES256bitCBC() throws NoSuchAlgorithmException, IOException {
		testCipher(new AES256Cbc(), new AES256Cbc());
	}
	
	public void testAES128bitCTR() throws NoSuchAlgorithmException, IOException {
		testCipher(new AES128Ctr(), new AES128Ctr());
	}
	
	public void testAES192bitCTR() throws NoSuchAlgorithmException, IOException {
		testCipher(new AES192Ctr(), new AES192Ctr());
	}
	
	public void testAES256bitCTR() throws NoSuchAlgorithmException, IOException {
		testCipher(new AES256Ctr(), new AES256Ctr());
	}
	
//	public void testAES128bitGCCM() throws NoSuchAlgorithmException, IOException {
//		testCipher(new AES128Gcm(), new AES128Gcm());
//	}
//	
//	public void testAES256bitGCCM() throws NoSuchAlgorithmException, IOException {
//		testCipher(new AES256Gcm(), new AES256Gcm());
//	}
	
	public void testArcfour128() throws NoSuchAlgorithmException, IOException {
		testCipher(new ArcFour128(), new ArcFour128());
	}
	
	public void testArcfour256() throws NoSuchAlgorithmException, IOException {
		testCipher(new ArcFour256(), new ArcFour256());
	}
	
	public void testBlowfish128CBC() throws NoSuchAlgorithmException, IOException {
		testCipher(new BlowfishCbc(), new BlowfishCbc());
	}
	
	public void test3DESCBC() throws NoSuchAlgorithmException, IOException {
		testCipher(new TripleDesCbc(), new TripleDesCbc());
	}
	
	public void test3DESCTR() throws NoSuchAlgorithmException, IOException {
		testCipher(new TripleDesCtr(), new TripleDesCtr());
	}
}
