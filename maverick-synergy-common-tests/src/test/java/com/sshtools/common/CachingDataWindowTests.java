package com.sshtools.common;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.Utils;
import com.sshtools.synergy.ssh.CachingDataWindow;

import junit.framework.TestCase;

public class CachingDataWindowTests extends TestCase {

	
	/**
	 * Loop placing data on the cache and reading it back again. Checking each time the buffer
	 * returned is exactly what was written
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void testInputIsOutput() throws NoSuchAlgorithmException, InterruptedException, IOException {
		
		
		final CachingDataWindow window = new CachingDataWindow(1024000, true);

		byte[] buffer = new byte[256];
		byte[] buffer2 = new byte[256];
		final Random r = new Random();
		ByteBuffer b = ByteBuffer.allocate(256);
		for(int i=0;i<10000;i++) {
			r.nextBytes(buffer);
			window.put(ByteBuffer.wrap(buffer));
			assertEquals(256, window.get(b));
			
			b.flip();
			b.get(buffer2);
			b.compact();
			System.out.println(Utils.bytesToHex(buffer, 64, true, false));
			System.out.println(Utils.bytesToHex(buffer2, 64, true, false));
			assertTrue("Source and Target arrays must be equal", Arrays.areEqual(buffer, buffer2));
		}
		
		
	}
	
	public void testClosedCacheOutput() {
		
		final CachingDataWindow window = new CachingDataWindow(1024000, true);

		byte[] buffer = new byte[256];
		final Random r = new Random();
		ByteBuffer b = ByteBuffer.allocate(256);
		
		r.nextBytes(buffer);
		try {
			window.put(ByteBuffer.wrap(buffer));
		} catch (EOFException e1) {
			fail();
		}
		
		window.close();
		
		try {
			assertEquals(256, window.get(b));
		} catch (EOFException e) {
			fail();
		}
		
		try {
			window.get(buffer, 0, buffer.length);
			fail();
		} catch(EOFException e) {
		}
		
		
	}
	/**
	 * Write data to the cache but read it in uneven chunks
	 */
	public void testRandomRead() throws NoSuchAlgorithmException {
		
		final CachingDataWindow window = new CachingDataWindow(1024000, true);
		

		final DigestOutputStream input = new DigestOutputStream(new OutputStream() { 
			public void write(int b) { }
		}, MessageDigest.getInstance("MD5"));
		
		final DigestOutputStream output = new DigestOutputStream(new OutputStream() { 
			public void write(int b) { }
		}, MessageDigest.getInstance("MD5"));
		
		final byte[] STATIC_TEXT = "All work and no play makes jack a dull boy!!!!\r\n".getBytes();
		
		new Thread() {
			public void run() {
				
				try {
					for(int i=0;i<10000;i++) {
						input.write(STATIC_TEXT);
						window.put(ByteBuffer.wrap(STATIC_TEXT));
					}
				} catch (IOException e) {
					e.printStackTrace();
					fail();
				} finally {
					window.close();
				}
				
			}
		}.start();
	
	
		byte[] b2 = new byte[100];
		while(window.isOpen()) {

			try {
			int count = window.get(ByteBuffer.wrap(b2));
				System.out.write(b2, 0, count);
				output.write(b2, 0, count);
			} catch (IOException e) {
				e.printStackTrace();
				fail();
			}
		}
		
		IOUtils.closeStream(input);
		IOUtils.closeStream(output);
		
		assertTrue("Source and Target digest must be equal",
				Arrays.areEqual(input.getMessageDigest().digest(), output.getMessageDigest().digest()));
	}
}
