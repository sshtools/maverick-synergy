package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.junit.Ignore;

import com.sshtools.common.ssh.components.AbstractSshCipher;
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
	
	protected void testCipher(AbstractSshCipher encrypt, AbstractSshCipher decrypt) throws IOException, NoSuchAlgorithmException {
		
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
		
		encrypt.init(AbstractSshCipher.ENCRYPT_MODE, iv, key);
		decrypt.init(AbstractSshCipher.DECRYPT_MODE, iv, key);
		
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
