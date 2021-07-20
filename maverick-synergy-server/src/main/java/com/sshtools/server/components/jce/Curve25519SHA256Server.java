
package com.sshtools.server.components.jce;

public class Curve25519SHA256Server extends Curve25519SHA256LibSshServer {

	public static final String CURVE25519_SHA2 = "curve25519-sha256";
	
	public Curve25519SHA256Server() {
		super(CURVE25519_SHA2);
	}	
}
