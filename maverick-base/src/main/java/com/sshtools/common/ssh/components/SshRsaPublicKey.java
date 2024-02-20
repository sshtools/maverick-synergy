package com.sshtools.common.ssh.components;

import java.math.BigInteger;
import java.security.PublicKey;

import com.sshtools.common.ssh.SshException;

/**
 * This interface should be implemented by all RSA public key implementations.
 * 
 * @author Lee David Painter
 */
public interface SshRsaPublicKey extends SshPublicKey {
	BigInteger getModulus();
	BigInteger getPublicExponent();
	int getVersion();
	public PublicKey getJCEPublicKey();
	public BigInteger doPublic(BigInteger input) throws SshException;
}
