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
