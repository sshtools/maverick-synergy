package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.SshHmacFactory;

/**
 * SHA-1 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacSha512_96 extends HmacSha512 {
	public static class HmacSha512_96Factory implements SshHmacFactory<HmacSha512_96> {
		@Override
		public HmacSha512_96 create() throws NoSuchAlgorithmException, IOException {
			return new HmacSha512_96();
		}

		@Override
		public String[] getKeys() {
			return new String[] { "hmac-sha2-512-96" };
		}
	}

	public HmacSha512_96() {
		super(12);
	}
	
	public String getAlgorithm() {
		return "hmac-sha2-512-96";
	}
}
