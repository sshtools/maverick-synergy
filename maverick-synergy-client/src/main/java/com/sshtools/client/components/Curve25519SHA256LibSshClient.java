
package com.sshtools.client.components;

public class Curve25519SHA256LibSshClient extends Curve25519SHA256Client {

	public static final String CURVE25519_SHA2_AT_LIBSSH_ORG = "curve25519-sha256@libssh.org";

	public Curve25519SHA256LibSshClient() {
		super(CURVE25519_SHA2_AT_LIBSSH_ORG);
	}
}
