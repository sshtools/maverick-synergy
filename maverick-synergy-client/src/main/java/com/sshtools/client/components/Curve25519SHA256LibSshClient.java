package com.sshtools.client.components;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.client.SshKeyExchangeClientFactory;

public class Curve25519SHA256LibSshClient extends Curve25519SHA256Client {
	
	public static class Curve25519SHA256LibSshClientFactory implements SshKeyExchangeClientFactory<Curve25519SHA256LibSshClient> {
		@Override
		public Curve25519SHA256LibSshClient create() throws NoSuchAlgorithmException, IOException {
			return new Curve25519SHA256LibSshClient();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CURVE25519_SHA2_AT_LIBSSH_ORG };
		}
	}

	public static final String CURVE25519_SHA2_AT_LIBSSH_ORG = "curve25519-sha256@libssh.org";

	public Curve25519SHA256LibSshClient() {
		super(CURVE25519_SHA2_AT_LIBSSH_ORG);
	}
}
