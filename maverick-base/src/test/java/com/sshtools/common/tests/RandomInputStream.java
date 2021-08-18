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
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class RandomInputStream extends InputStream {

		long totalDataAmount;
		int maximumBlockSize;
		Random r = new Random();
		MessageDigest digest;
		boolean randomBlock;
		
		RandomInputStream(int maximumBlockSize, long totalDataAmount, boolean randomBlock) throws NoSuchAlgorithmException {
			this.maximumBlockSize = maximumBlockSize;
			this.totalDataAmount = totalDataAmount;
			this.randomBlock = randomBlock;
			this.digest = MessageDigest.getInstance("MD5");
		}
		
		public int read(byte[] buf, int off, int len) {
		
			if(totalDataAmount==0) {
				return -1;
			}
			int max = Math.min(len, maximumBlockSize);
			if(totalDataAmount < max) {
				max = (int) totalDataAmount;
			}
			int s = max;
			
			if(randomBlock) {
				s = r.nextInt(max);
				if(s==0) {
					s = max;
				}
			}
			byte[] b = new byte[s];
			r.nextBytes(b);
			
			digest.update(b);
			System.arraycopy(b, 0, buf, off, b.length);
			totalDataAmount-=b.length;
			return b.length;
		}
		
		@Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			if(read(b) > 0) {
				return b[1] & 0xFF;
			};
			return -1;
		}

		public byte[] digest() {
			return digest.digest();
		}
		
	}