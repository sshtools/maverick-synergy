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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshCipherFactory;

public class ArcFour128 extends AbstractJCECipher {

	private static final String CIPHER = "arcfour128";

	public static class ArcFour128Factory implements SshCipherFactory<ArcFour128> {

		@Override
		public ArcFour128 create() throws NoSuchAlgorithmException, IOException {
			return new ArcFour128();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CIPHER };
		}

		@Override
		public boolean isEnabledByDefault() {
			return false;
		}
	}
	
	public ArcFour128() throws IOException {
		super("ARCFOUR", "ARCFOUR", 16, CIPHER, SecurityLevel.WEAK, 0);	
	}

	public void init(int mode, byte[] iv, byte[] keydata) throws IOException {
	      try {

	          cipher = JCEProvider.getProviderForAlgorithm(spec)==null ?
	              	Cipher.getInstance(spec)
	              	: Cipher.getInstance(spec, JCEProvider.getProviderForAlgorithm(spec));

	          if(cipher==null) {
	              throw new IOException("Failed to create cipher engine for "
	                                    + spec);
	          }

	          // Create a byte key
	          byte[] actualKey = new byte[keylength];
	          System.arraycopy(keydata, 0, actualKey, 0, actualKey.length);

	          SecretKeySpec kspec = new SecretKeySpec(actualKey, keyspec);

	          // Create the cipher according to its algorithm
	          cipher.init(((mode == ENCRYPT_MODE) ? Cipher.ENCRYPT_MODE
	                       : Cipher.DECRYPT_MODE),
	                      kspec);
	          
	          byte[] tmp = new byte[1536];
	          cipher.update(tmp);
	          
	      } catch (NoSuchPaddingException nspe) {
	          throw new IOException("Padding type not supported");
	      } catch (NoSuchAlgorithmException nsae) {
	          throw new IOException("Algorithm not supported:"+spec);
	      } catch (InvalidKeyException ike) {
	          throw new IOException("Invalid encryption key");
	      } 
	}

	public int getBlockSize() {
		return 8;
	}
}
