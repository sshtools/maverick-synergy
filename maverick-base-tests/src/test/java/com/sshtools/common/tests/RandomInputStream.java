/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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