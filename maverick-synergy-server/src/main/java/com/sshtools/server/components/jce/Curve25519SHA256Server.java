package com.sshtools.server.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.server.components.SshKeyExchangeServerFactory;

public class Curve25519SHA256Server extends Curve25519SHA256LibSshServer {

	public static final String CURVE25519_SHA2 = "curve25519-sha256";

	public static class Curve25519SHA256ServerFactory implements SshKeyExchangeServerFactory<Curve25519SHA256Server> {
		@Override
		public Curve25519SHA256Server create() throws NoSuchAlgorithmException, IOException {
			return new Curve25519SHA256Server();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CURVE25519_SHA2 };
		}
	}
	
	public Curve25519SHA256Server() {
		super(CURVE25519_SHA2);
	}	
}
