package com.sshtools.common.zlib;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.compression.SshCompressionFactory;

public class OpenSSHZLibCompression extends ZLibCompression {
	
	private static final String ALGORITHM = "zlib@openssh.com";

	public static class OpenSSHZLibCompressionFactory implements SshCompressionFactory<OpenSSHZLibCompression> {

		@Override
		public OpenSSHZLibCompression create() throws NoSuchAlgorithmException, IOException {
			return new OpenSSHZLibCompression();
		}

		@Override
		public String[] getKeys() {
			return new String[] { ALGORITHM };
		}
	}

	public String getAlgorithm() {
		return ALGORITHM;
	}
}
