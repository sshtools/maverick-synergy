package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.SshHmacFactory;

/**
 * SHA-1 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacSha256_96 extends HmacSha256 {
	
	public static class HmacSha256_96Factory implements SshHmacFactory<HmacSha256_96> {
		@Override
		public HmacSha256_96 create() throws NoSuchAlgorithmException, IOException {
			return new HmacSha256_96();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  "hmac-sha2-256-96" };
		}
	}

	public HmacSha256_96() {
		super(12);
	}
	
	public String getAlgorithm() {
		return "hmac-sha2-256-96";
	}
}
