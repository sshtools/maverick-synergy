package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.math.BigInteger;

/**
 * This interface should be implemented by all RSA private key
 * implementations.
 * @author Lee David Painter
 */
public interface SshRsaPrivateKey extends SshPrivateKey {

	public abstract BigInteger getModulus();
	
	public abstract BigInteger getPrivateExponent();

	public abstract byte[] sign(byte[] msg) throws IOException;

}